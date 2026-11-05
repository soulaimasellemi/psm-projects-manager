package com.smartsoft.psm.dto;

import java.util.List;
import java.util.Map;

/** Statistiques du tableau de bord. */
public record StatsDto(long nbClients, long nbProjets, double budgetTotal, long nbUsers, long usersEnAttente,
                       Map<String, Long> projetsParStatut, List<TopClient> topClients,
                       List<ProjetDto> derniersProjets) {

    public record TopClient(String nom, double budget) {}
}
