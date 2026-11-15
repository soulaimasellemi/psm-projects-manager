package com.smartsoft.psm.dto;

import com.smartsoft.psm.entity.Projet;
import com.smartsoft.psm.entity.StatutProjet;
import com.smartsoft.psm.entity.Tache;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Santé d'un projet : score de 0 (critique) à 100 (sain), niveau et raisons lisibles.
 *
 * Chaque signal de risque retire des points :
 *  - fin prévue dépassée (projet non terminé)                     -40
 *  - avancement en retard sur le temps écoulé (écart > 20 points) -25
 *  - échéance proche (< 14 jours) avec avancement < 80 %          -15
 *  - tâches en retard (échéance dépassée)                          -10 par tâche, max -30
 *  - tâches haute priorité non assignées                           -10
 *  - projet en cours sans aucun membre affecté                     -15
 * Niveau : SAIN (>= 80), A_SURVEILLER (50 à 79), CRITIQUE (< 50).
 */
public record SanteDto(int score, Niveau niveau, List<String> raisons) {

    public enum Niveau { SAIN, A_SURVEILLER, CRITIQUE }

    private static final int ECHEANCE_PROCHE_JOURS = 14;

    public static SanteDto calculer(Projet p) {
        if (p.getStatut() == StatutProjet.TERMINE) {
            return new SanteDto(100, Niveau.SAIN, List.of("Projet terminé"));
        }
        LocalDate today = LocalDate.now();
        List<String> raisons = new ArrayList<>();
        int risque = 0;

        // 1. Fin prévue dépassée
        if (p.isEnRetard()) {
            long jours = ChronoUnit.DAYS.between(p.getDateFinPrevue(), today);
            risque += 40;
            raisons.add("Fin prévue dépassée de " + jours + " jour" + (jours > 1 ? "s" : ""));
        }

        // 2. Avancement en retard par rapport au temps écoulé
        LocalDate debut = p.getDateDebut();
        LocalDate fin = p.getDateFinPrevue();
        if (debut != null && fin != null && fin.isAfter(debut) && !today.isBefore(debut) && !p.isEnRetard()) {
            double total = ChronoUnit.DAYS.between(debut, fin);
            int tempsEcoule = (int) Math.round(Math.min(1.0, ChronoUnit.DAYS.between(debut, today) / total) * 100);
            if (tempsEcoule - p.getAvancement() > 20) {
                risque += 25;
                raisons.add("Avancement de " + p.getAvancement() + " % alors que " + tempsEcoule + " % du temps est écoulé");
            }
        }

        // 3. Échéance proche et travail restant important
        if (fin != null && !p.isEnRetard()) {
            long joursRestants = ChronoUnit.DAYS.between(today, fin);
            if (joursRestants <= ECHEANCE_PROCHE_JOURS && p.getAvancement() < 80) {
                risque += 15;
                raisons.add("Fin prévue dans " + joursRestants + " jour" + (joursRestants > 1 ? "s" : "")
                        + " avec seulement " + p.getAvancement() + " % réalisé");
            }
        }

        // 4. Tâches en retard
        long tachesEnRetard = p.getTaches().stream().filter(Tache::isEnRetard).count();
        if (tachesEnRetard > 0) {
            risque += (int) Math.min(30, tachesEnRetard * 10);
            raisons.add(tachesEnRetard + " tâche" + (tachesEnRetard > 1 ? "s" : "") + " en retard");
        }

        // 5. Tâches importantes sans responsable
        long urgentesSansResponsable = p.getTaches().stream()
                .filter(t -> t.getPriorite() == Tache.Priorite.HAUTE && t.getAssigne() == null
                        && t.getStatut() != Tache.Statut.TERMINEE)
                .count();
        if (urgentesSansResponsable > 0) {
            risque += 10;
            raisons.add(urgentesSansResponsable + " tâche" + (urgentesSansResponsable > 1 ? "s" : "")
                    + " haute priorité sans responsable");
        }

        // 6. Projet en cours sans équipe
        if (p.getStatut() == StatutProjet.EN_COURS && p.getMembres().isEmpty()) {
            risque += 15;
            raisons.add("Aucun collaborateur affecté");
        }

        int score = Math.max(0, 100 - risque);
        Niveau niveau = score >= 80 ? Niveau.SAIN : score >= 50 ? Niveau.A_SURVEILLER : Niveau.CRITIQUE;
        if (raisons.isEmpty()) raisons.add("Aucun risque détecté");
        return new SanteDto(score, niveau, raisons);
    }
}
