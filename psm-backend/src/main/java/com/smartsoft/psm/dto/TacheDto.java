package com.smartsoft.psm.dto;

import com.smartsoft.psm.entity.Tache;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Tâche du Kanban renvoyée par l'API. */
public record TacheDto(Long id, String titre, String description, Tache.Statut statut, Tache.Priorite priorite,
                       LocalDate echeance, int ordre, boolean enRetard, Long projetId,
                       ProjetDto.Membre assigne, LocalDateTime dateCreation, LocalDateTime dateTerminee) {

    public static TacheDto from(Tache t) {
        return new TacheDto(t.getId(), t.getTitre(), t.getDescription(), t.getStatut(), t.getPriorite(),
                t.getEcheance(), t.getOrdre(), t.isEnRetard(), t.getProjet().getId(),
                t.getAssigne() == null ? null : ProjetDto.Membre.from(t.getAssigne()),
                t.getDateCreation(), t.getDateTerminee());
    }

    /** Création / modification d'une tâche */
    public record TacheRequest(
            @NotBlank(message = "Le titre est obligatoire") @Size(max = 150) String titre,
            @Size(max = 1000) String description,
            Tache.Statut statut,
            Tache.Priorite priorite,
            LocalDate echeance,
            Long assigneId) {}

    /** Glisser-déposer sur le Kanban : nouvelle colonne et nouvelle position */
    public record DeplacementRequest(
            @NotNull(message = "La colonne est obligatoire") Tache.Statut statut,
            @NotNull @Min(0) Integer ordre) {}
}
