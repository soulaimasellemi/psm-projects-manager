package com.smartsoft.psm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Historique des actions (journal d'audit) : qui a fait quoi, sur quoi et quand.
 * L'auteur est copié en texte (email + nom) pour que l'historique reste lisible
 * même après la suppression du compte.
 */
@Entity
@Table(name = "activites", indexes = @Index(name = "idx_activite_date", columnList = "dateAction"))
public class Activite {

    /** Type d'action enregistrée */
    public enum Action { CREATION, MODIFICATION, SUPPRESSION, CONNEXION, INSCRIPTION, VALIDATION, MOT_DE_PASSE }

    /** Ressource concernée */
    public enum Cible { CLIENT, PROJET, TACHE, UTILISATEUR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dateAction;

    @Column(length = 120)
    private String auteurEmail;

    @Column(length = 170)
    private String auteurNom;

    // VARCHAR (et non ENUM SQL) : ajouter une valeur à l'enum Java ne nécessite pas de migration
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Action action;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Cible cible;

    private Long cibleId;

    @Column(length = 500)
    private String details;

    public Activite() {}

    public Activite(String auteurEmail, String auteurNom, Action action, Cible cible, Long cibleId, String details) {
        this.dateAction = LocalDateTime.now();
        this.auteurEmail = auteurEmail;
        this.auteurNom = auteurNom;
        this.action = action;
        this.cible = cible;
        this.cibleId = cibleId;
        this.details = details == null || details.length() <= 500 ? details : details.substring(0, 497) + "...";
    }

    public Long getId() { return id; }
    public LocalDateTime getDateAction() { return dateAction; }
    public String getAuteurEmail() { return auteurEmail; }
    public String getAuteurNom() { return auteurNom; }
    public Action getAction() { return action; }
    public Cible getCible() { return cible; }
    public Long getCibleId() { return cibleId; }
    public String getDetails() { return details; }
}
