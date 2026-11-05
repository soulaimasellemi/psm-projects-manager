package com.smartsoft.psm.dto;

import com.smartsoft.psm.entity.Projet;
import com.smartsoft.psm.entity.StatutProjet;
import jakarta.validation.constraints.*;

public record ProjetDto(Long id, String libelle, Double budget, Integer duree, String description,
                        StatutProjet statut, Long idClient, String nomClient) {

    public static ProjetDto from(Projet p) {
        return new ProjetDto(p.getId(), p.getLibelle(), p.getBudget(), p.getDuree(), p.getDescription(),
                p.getStatut(), p.getClient().getId(), p.getClient().getNom());
    }

    /** Corps des requêtes POST / PUT */
    public record ProjetRequest(
            @NotBlank(message = "Le libellé est obligatoire") @Size(max = 150) String libelle,
            @NotNull(message = "Le budget est obligatoire") @PositiveOrZero(message = "Le budget doit être positif") Double budget,
            @NotNull(message = "La durée est obligatoire") @Min(value = 1, message = "La durée minimale est 1 mois") Integer duree,
            @Size(max = 2000) String description,
            StatutProjet statut,
            @NotNull(message = "Le client est obligatoire") Long idClient) {}
}
