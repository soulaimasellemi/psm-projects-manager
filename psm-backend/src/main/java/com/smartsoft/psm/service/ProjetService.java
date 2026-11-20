package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.dto.ProjetDto;
import com.smartsoft.psm.dto.ProjetDto.ProjetRequest;
import com.smartsoft.psm.entity.Activite.Action;
import com.smartsoft.psm.entity.Activite.Cible;
import com.smartsoft.psm.entity.Projet;
import com.smartsoft.psm.entity.StatutProjet;
import com.smartsoft.psm.entity.User;
import com.smartsoft.psm.exception.ApiException;
import com.smartsoft.psm.repository.ProjetRepository;
import com.smartsoft.psm.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProjetService {

    /** Filtres de la liste des projets (tous facultatifs) */
    public record Filtres(String q, StatutProjet statut, Long clientId, Long membreId, boolean enRetard) {
        String texte() { return q == null ? "" : q.trim(); }
        Boolean retard() { return enRetard ? Boolean.TRUE : null; }
    }

    private final ProjetRepository projetRepository;
    private final ClientService clientService;
    private final UserRepository userRepository;
    private final ActiviteService activiteService;
    private final ExportService exportService;

    public ProjetService(ProjetRepository projetRepository, ClientService clientService,
                         UserRepository userRepository, ActiviteService activiteService, ExportService exportService) {
        this.projetRepository = projetRepository;
        this.clientService = clientService;
        this.userRepository = userRepository;
        this.activiteService = activiteService;
        this.exportService = exportService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjetDto> findAll(Filtres f, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending());
        return PageResponse.of(projetRepository.search(f.texte(), f.statut(), f.clientId(), f.membreId(), f.retard(),
                LocalDate.now(), pageable).map(ProjetDto::from));
    }

    /** Export Excel / PDF des projets correspondant aux filtres */
    @Transactional(readOnly = true)
    public ExportService.Fichier exporter(Filtres f, ExportService.Format format) {
        List<Projet> projets = projetRepository.searchAll(f.texte(), f.statut(), f.clientId(), f.membreId(), f.retard(),
                LocalDate.now(), Sort.by("client.nom").ascending().and(Sort.by("libelle").ascending()));
        return exportService.exporterProjets(projets, format);
    }

    @Transactional(readOnly = true)
    public ProjetDto findById(Long id) {
        return ProjetDto.from(getEntity(id));
    }

    public ProjetDto create(ProjetRequest req) {
        Projet p = new Projet();
        apply(p, req);
        Projet saved = projetRepository.save(p);
        activiteService.enregistrer(Action.CREATION, Cible.PROJET, saved.getId(),
                "Projet « " + saved.getLibelle() + " » pour " + saved.getClient().getNom());
        return ProjetDto.from(saved);
    }

    public ProjetDto update(Long id, ProjetRequest req) {
        Projet p = getEntity(id);
        apply(p, req);
        activiteService.enregistrer(Action.MODIFICATION, Cible.PROJET, p.getId(),
                "Projet « " + p.getLibelle() + " » (" + libelleStatut(p.getStatut()) + ", " + p.getAvancement() + " %)");
        return ProjetDto.from(p);
    }

    public void delete(Long id) {
        Projet p = getEntity(id);
        p.getClient().getProjets().remove(p);
        projetRepository.delete(p);
        activiteService.enregistrer(Action.SUPPRESSION, Cible.PROJET, id,
                "Projet « " + p.getLibelle() + " » (" + p.getClient().getNom() + ")");
    }

    private static String libelleStatut(StatutProjet s) {
        return switch (s) {
            case EN_ATTENTE -> "en attente";
            case EN_COURS -> "en cours";
            case TERMINE -> "terminé";
        };
    }

    private Projet getEntity(Long id) {
        return projetRepository.findById(id).orElseThrow(() -> ApiException.notFound("Projet introuvable (id=" + id + ")"));
    }

    private void apply(Projet p, ProjetRequest req) {
        if (req.dateDebut() != null && req.dateFinPrevue() != null && req.dateFinPrevue().isBefore(req.dateDebut())) {
            throw ApiException.badRequest("La date de fin prévue doit être postérieure à la date de début.");
        }
        StatutProjet statut = req.statut() == null ? StatutProjet.EN_ATTENTE : req.statut();
        int avancement = req.avancement() == null ? 0 : req.avancement();

        p.setLibelle(req.libelle().trim());
        p.setBudget(req.budget());
        p.setDuree(req.duree());
        p.setDescription(ClientService.blankToNull(req.description()));
        p.setStatut(statut);
        p.setClient(clientService.getEntity(req.idClient()));
        p.setDateDebut(req.dateDebut());
        p.setDateFinPrevue(req.dateFinPrevue());
        p.setMembres(resoudreMembres(req.membresIds(), p.getMembres()));
        // Un membre retiré de l'équipe n'est plus responsable des tâches du projet
        p.getTaches().stream()
                .filter(t -> t.getAssigne() != null && !p.getMembres().contains(t.getAssigne()))
                .forEach(t -> t.setAssigne(null));
        // Avancement : saisi à la main, sauf si le projet a des tâches (calcul automatique) ou est terminé (100 %)
        p.setAvancement(avancement);
        p.recalculerAvancement();
    }

    /**
     * Seuls des comptes actifs peuvent être NOUVELLEMENT affectés à un projet
     * (un membre déjà présent puis désactivé peut rester affecté).
     */
    private Set<User> resoudreMembres(Set<Long> ids, Set<User> actuels) {
        Set<User> membres = new HashSet<>();
        if (ids == null) return membres;
        for (Long id : ids) {
            User u = userRepository.findById(id)
                    .orElseThrow(() -> ApiException.badRequest("Collaborateur introuvable (id=" + id + ")"));
            if (!u.isEnabled() && !actuels.contains(u)) {
                throw ApiException.badRequest(u.getNomComplet() + " n'a pas de compte actif.");
            }
            membres.add(u);
        }
        return membres;
    }
}
