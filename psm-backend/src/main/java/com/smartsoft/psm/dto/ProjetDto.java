package com.smartsoft.psm.dto;

import com.smartsoft.psm.entity.Projet;
import com.smartsoft.psm.entity.StatutProjet;
import com.smartsoft.psm.entity.Tache;
import com.smartsoft.psm.entity.User;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public record ProjetDto(Long id, String libelle, Double budget, Integer duree, String description,
                        StatutProjet statut, Long idClient, String nomClient,
                        LocalDate dateDebut, LocalDate dateFinPrevue, int avancement, boolean enRetard,
                        List<Membre> membres, int nbTaches, int nbTachesTerminees, SanteDto sante) {

    /** Collaborateur affecté au projet */
    public record Membre(Long id, String nomComplet, String email) {
        public static Membre from(User u) {
            return new Membre(u.getId(), u.getNomComplet(), u.getEmail());
        }
    }

    public static ProjetDto from(Projet p) {
        List<Membre> membres = p.getMembres().stream()
                .sorted(Comparator.comparing(User::getPrenom).thenComparing(User::getNom))
                .map(Membre::from)
                .toList();
        return new ProjetDto(p.getId(), p.getLibelle(), p.getBudget(), p.getDuree(), p.getDescription(),
                p.getStatut(), p.getClient().getId(), p.getClient().getNom(),
                p.getDateDebut(), p.getDateFinPrevue(), p.getAvancement(), p.isEnRetard(), membres,
                p.getTaches().size(),
                (int) p.getTaches().stream().filter(t -> t.getStatut() == Tache.Statut.TERMINEE).count(),
                SanteDto.calculer(p));
    }

    /** Corps des requêtes POST / PUT */
    public record ProjetRequest(
            @NotBlank(message = "Le libellé est obligatoire") @Size(max = 150) String libelle,
            @NotNull(message = "Le budget est obligatoire") @PositiveOrZero(message = "Le budget doit être positif") Double budget,
            @NotNull(message = "La durée est obligatoire") @Min(value = 1, message = "La durée minimale est 1 mois") Integer duree,
            @Size(max = 2000) String description,
            StatutProjet statut,
            @NotNull(message = "Le client est obligatoire") Long idClient,
            LocalDate dateDebut,
            LocalDate dateFinPrevue,
            @Min(value = 0, message = "L'avancement est compris entre 0 et 100 %")
            @Max(value = 100, message = "L'avancement est compris entre 0 et 100 %") Integer avancement,
            Set<Long> membresIds) {}
}
