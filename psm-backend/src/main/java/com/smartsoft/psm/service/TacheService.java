package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.TacheDto;
import com.smartsoft.psm.dto.TacheDto.DeplacementRequest;
import com.smartsoft.psm.dto.TacheDto.TacheRequest;
import com.smartsoft.psm.entity.Activite.Action;
import com.smartsoft.psm.entity.Activite.Cible;
import com.smartsoft.psm.entity.Projet;
import com.smartsoft.psm.entity.Role;
import com.smartsoft.psm.entity.Tache;
import com.smartsoft.psm.entity.User;
import com.smartsoft.psm.exception.ApiException;
import com.smartsoft.psm.repository.ProjetRepository;
import com.smartsoft.psm.repository.TacheRepository;
import com.smartsoft.psm.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tâches du tableau Kanban.
 * Droits : l'ADMIN gère toutes les tâches ; un COLLABORATEUR peut créer, modifier et déplacer
 * les tâches des projets dont il est membre. La suppression est réservée à l'ADMIN (SecurityConfig).
 * Après chaque changement, l'avancement du projet est recalculé (% de tâches terminées).
 */
@Service
@Transactional
public class TacheService {

    private final TacheRepository tacheRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final ActiviteService activiteService;

    public TacheService(TacheRepository tacheRepository, ProjetRepository projetRepository,
                        UserRepository userRepository, ActiviteService activiteService) {
        this.tacheRepository = tacheRepository;
        this.projetRepository = projetRepository;
        this.userRepository = userRepository;
        this.activiteService = activiteService;
    }

    @Transactional(readOnly = true)
    public List<TacheDto> findByProjet(Long projetId) {
        return getProjet(projetId).getTaches().stream()
                .sorted(Comparator.comparingInt(Tache::getOrdre).thenComparing(Tache::getId))
                .map(TacheDto::from)
                .toList();
    }

    public TacheDto create(Long projetId, TacheRequest req, User current) {
        Projet p = getProjet(projetId);
        verifierAcces(p, current);
        Tache t = new Tache();
        t.setProjet(p);
        appliquer(t, req, p);
        t.setOrdre(colonne(p, t.getStatut()).size()); // en bas de sa colonne
        p.getTaches().add(t);
        tacheRepository.save(t);
        p.recalculerAvancement();
        activiteService.enregistrer(Action.CREATION, Cible.TACHE, t.getId(),
                "Tâche « " + t.getTitre() + " » dans le projet « " + p.getLibelle() + " »");
        return TacheDto.from(t);
    }

    public TacheDto update(Long id, TacheRequest req, User current) {
        Tache t = getEntity(id);
        Projet p = t.getProjet();
        verifierAcces(p, current);
        Tache.Statut ancien = t.getStatut();
        appliquer(t, req, p);
        if (ancien != t.getStatut()) {
            t.setOrdre(colonne(p, t.getStatut()).size() - 1);
            renumeroter(colonne(p, ancien));
        }
        p.recalculerAvancement();
        journaliserChangement(t, ancien, "Tâche « " + t.getTitre() + " » modifiée");
        return TacheDto.from(t);
    }

    /** Glisser-déposer : change de colonne et/ou de position, puis renumérote les colonnes concernées. */
    public TacheDto deplacer(Long id, DeplacementRequest req, User current) {
        Tache t = getEntity(id);
        Projet p = t.getProjet();
        verifierAcces(p, current);
        Tache.Statut ancien = t.getStatut();

        List<Tache> source = colonne(p, ancien);
        source.remove(t);
        renumeroter(source);

        t.setStatut(req.statut());
        List<Tache> cible = ancien == req.statut() ? source : colonne(p, req.statut());
        cible.remove(t);
        cible.add(Math.min(req.ordre(), cible.size()), t);
        renumeroter(cible);

        p.recalculerAvancement();
        if (ancien != t.getStatut()) {
            journaliserChangement(t, ancien, null);
        }
        return TacheDto.from(t);
    }

    public void delete(Long id) {
        Tache t = getEntity(id);
        Projet p = t.getProjet();
        p.getTaches().remove(t); // orphanRemoval : supprimée en base
        renumeroter(colonne(p, t.getStatut()));
        p.recalculerAvancement();
        activiteService.enregistrer(Action.SUPPRESSION, Cible.TACHE, id,
                "Tâche « " + t.getTitre() + " » du projet « " + p.getLibelle() + " »");
    }

    // ---------- Outils ----------

    private void appliquer(Tache t, TacheRequest req, Projet p) {
        t.setTitre(req.titre().trim());
        t.setDescription(ClientService.blankToNull(req.description()));
        t.setPriorite(req.priorite() == null ? Tache.Priorite.MOYENNE : req.priorite());
        t.setEcheance(req.echeance());
        if (req.statut() != null) t.setStatut(req.statut());
        if (req.assigneId() == null) {
            t.setAssigne(null);
        } else {
            User u = userRepository.findById(req.assigneId())
                    .orElseThrow(() -> ApiException.badRequest("Utilisateur introuvable (id=" + req.assigneId() + ")"));
            if (p.getMembres().stream().noneMatch(m -> m.getId().equals(u.getId()))) {
                throw ApiException.badRequest(u.getNomComplet() + " ne fait pas partie de l'équipe du projet.");
            }
            t.setAssigne(u);
        }
    }

    /** ADMIN : tous les projets. COLLABORATEUR : uniquement les projets dont il est membre. */
    private static void verifierAcces(Projet p, User current) {
        if (current == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        if (current.hasRole(Role.ADMIN)) return;
        boolean membre = p.getMembres().stream().anyMatch(m -> m.getId().equals(current.getId()));
        if (!membre) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Seuls les membres de l'équipe du projet peuvent gérer ses tâches.");
        }
    }

    /** Tâches d'une colonne, dans l'ordre d'affichage (liste modifiable) */
    private static List<Tache> colonne(Projet p, Tache.Statut statut) {
        return new ArrayList<>(p.getTaches().stream()
                .filter(t -> t.getStatut() == statut)
                .sorted(Comparator.comparingInt(Tache::getOrdre).thenComparing(t -> t.getId() == null ? Long.MAX_VALUE : t.getId()))
                .toList());
    }

    private static void renumeroter(List<Tache> colonne) {
        for (int i = 0; i < colonne.size(); i++) colonne.get(i).setOrdre(i);
    }

    private void journaliserChangement(Tache t, Tache.Statut ancien, String detailSiInchange) {
        String projet = " (projet « " + t.getProjet().getLibelle() + " », avancement " + t.getProjet().getAvancement() + " %)";
        if (ancien != t.getStatut()) {
            String libelle = switch (t.getStatut()) {
                case A_FAIRE -> "remise à faire";
                case EN_COURS -> "démarrée";
                case TERMINEE -> "terminée";
            };
            activiteService.enregistrer(Action.MODIFICATION, Cible.TACHE, t.getId(),
                    "Tâche « " + t.getTitre() + " » " + libelle + projet);
        } else if (detailSiInchange != null) {
            activiteService.enregistrer(Action.MODIFICATION, Cible.TACHE, t.getId(), detailSiInchange + projet);
        }
    }

    private Projet getProjet(Long id) {
        return projetRepository.findById(id).orElseThrow(() -> ApiException.notFound("Projet introuvable (id=" + id + ")"));
    }

    private Tache getEntity(Long id) {
        return tacheRepository.findById(id).orElseThrow(() -> ApiException.notFound("Tâche introuvable (id=" + id + ")"));
    }
}
