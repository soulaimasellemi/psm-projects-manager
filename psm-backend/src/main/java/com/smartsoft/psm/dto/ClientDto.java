package com.smartsoft.psm.dto;

import com.smartsoft.psm.entity.Client;
import com.smartsoft.psm.entity.Projet;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientDto(Long id, String nom, String tel, String email, String adresse, String secteur,
                        int nbProjets, double budgetTotal) {

    public static ClientDto from(Client c) {
        double total = c.getProjets().stream().mapToDouble(Projet::getBudget).sum();
        return new ClientDto(c.getId(), c.getNom(), c.getTel(), c.getEmail(), c.getAdresse(), c.getSecteur(),
                c.getProjets().size(), total);
    }

    /** Corps des requêtes POST / PUT */
    public record ClientRequest(
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 120) String nom,
            @Size(max = 30) String tel,
            @Email(message = "Email invalide") @Size(max = 120) String email,
            @Size(max = 255) String adresse,
            @Size(max = 60) String secteur) {}

    /** Version courte pour les listes déroulantes */
    public record ClientOption(Long id, String nom) {}
}
