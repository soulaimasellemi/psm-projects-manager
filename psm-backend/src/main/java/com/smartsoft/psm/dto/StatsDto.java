package com.smartsoft.psm.dto;

import java.util.List;
import java.util.Map;

/** Statistiques du tableau de bord. */
public record StatsDto(long nbClients, long nbProjets, double budgetTotal, long nbUsers, long usersEnAttente,
                       Map<String, Long> projetsParStatut, List<TopClient> topClients,
                       List<ProjetDto> derniersProjets,
                       long projetsEnRetard, int avancementMoyen, List<Avancement> avancements,
                       long mesProjets,
                       int santeMoyenne, long nbProjetsCritiques, long nbProjetsASurveiller,
                       List<ProjetRisque> projetsARisque) {

    /** Projet non terminé classé par score de santé (les plus à risque d'abord) */
    public record ProjetRisque(Long id, String libelle, String nomClient, int score, SanteDto.Niveau niveau,
                               List<String> raisons) {}

    public record TopClient(String nom, double budget) {}

    /** Avancement d'un projet en cours (graphique du dashboard) */
    public record Avancement(Long id, String libelle, int avancement, boolean enRetard) {}
}
