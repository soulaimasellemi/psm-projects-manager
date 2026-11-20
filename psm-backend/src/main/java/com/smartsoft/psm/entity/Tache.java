package com.smartsoft.psm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tâche d'un projet, affichée sur le tableau Kanban (À faire / En cours / Terminée).
 * L'avancement du projet est calculé à partir de ses tâches terminées.
 */
@Entity
@Table(name = "taches", indexes = @Index(name = "idx_tache_projet", columnList = "projet_id"))
public class Tache {

    public enum Statut { A_FAIRE, EN_COURS, TERMINEE }

    public enum Priorite { BASSE, MOYENNE, HAUTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.A_FAIRE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Priorite priorite = Priorite.MOYENNE;

    /** Date limite (facultative) */
    private LocalDate echeance;

    /** Position dans sa colonne du Kanban (0 = en haut) */
    @Column(nullable = false)
    private int ordre;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    /** Membre de l'équipe chargé de la tâche (facultatif) */
    @ManyToOne
    @JoinColumn(name = "assigne_id")
    private User assigne;

    private LocalDateTime dateCreation;

    /** Renseignée quand la tâche passe à "Terminée" */
    private LocalDateTime dateTerminee;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }

    /** En retard : échéance dépassée et tâche non terminée */
    public boolean isEnRetard() {
        return echeance != null && statut != Statut.TERMINEE && echeance.isBefore(LocalDate.now());
    }

    public Long getId() { return id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) {
        if (statut == Statut.TERMINEE && this.statut != Statut.TERMINEE) dateTerminee = LocalDateTime.now();
        if (statut != Statut.TERMINEE) dateTerminee = null;
        this.statut = statut;
    }
    public Priorite getPriorite() { return priorite; }
    public void setPriorite(Priorite priorite) { this.priorite = priorite; }
    public LocalDate getEcheance() { return echeance; }
    public void setEcheance(LocalDate echeance) { this.echeance = echeance; }
    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
    public Projet getProjet() { return projet; }
    public void setProjet(Projet projet) { this.projet = projet; }
    public User getAssigne() { return assigne; }
    public void setAssigne(User assigne) { this.assigne = assigne; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateTerminee() { return dateTerminee; }
}
