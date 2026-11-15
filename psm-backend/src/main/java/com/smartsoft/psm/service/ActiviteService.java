package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.ActiviteDto;
import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.entity.Activite;
import com.smartsoft.psm.entity.Activite.Action;
import com.smartsoft.psm.entity.Activite.Cible;
import com.smartsoft.psm.entity.User;
import com.smartsoft.psm.repository.ActiviteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Journal d'audit : enregistre qui a créé / modifié / supprimé quoi, et quand.
 * L'écriture se fait dans la même transaction que l'action : si l'action échoue,
 * rien n'est enregistré dans l'historique.
 */
@Service
@Transactional
public class ActiviteService {

    private final ActiviteRepository activiteRepository;

    public ActiviteService(ActiviteRepository activiteRepository) {
        this.activiteRepository = activiteRepository;
    }

    /** Enregistre une action faite par l'utilisateur connecté. */
    public void enregistrer(Action action, Cible cible, Long cibleId, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            enregistrerPour(u, action, cible, cibleId, details);
        } else {
            activiteRepository.save(new Activite(null, "Système", action, cible, cibleId, details));
        }
    }

    /** Enregistre une action dont l'auteur est connu explicitement (connexion, inscription...). */
    public void enregistrerPour(User auteur, Action action, Cible cible, Long cibleId, String details) {
        activiteRepository.save(new Activite(auteur.getEmail(), auteur.getNomComplet(), action, cible, cibleId, details));
    }

    @Transactional(readOnly = true)
    public PageResponse<ActiviteDto> findAll(String q, Cible cible, Action action, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("dateAction").descending().and(Sort.by("id").descending()));
        return PageResponse.of(activiteRepository.search(q == null ? "" : q.trim(), cible, action, pageable)
                .map(ActiviteDto::from));
    }
}
