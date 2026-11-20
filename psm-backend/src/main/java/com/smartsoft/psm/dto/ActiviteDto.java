package com.smartsoft.psm.dto;

import com.smartsoft.psm.entity.Activite;

import java.time.LocalDateTime;

/** Ligne de l'historique des actions. */
public record ActiviteDto(Long id, LocalDateTime dateAction, String auteurEmail, String auteurNom,
                          Activite.Action action, Activite.Cible cible, Long cibleId, String details) {

    public static ActiviteDto from(Activite a) {
        return new ActiviteDto(a.getId(), a.getDateAction(), a.getAuteurEmail(), a.getAuteurNom(),
                a.getAction(), a.getCible(), a.getCibleId(), a.getDetails());
    }
}
