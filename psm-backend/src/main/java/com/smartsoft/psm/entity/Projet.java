package com.smartsoft.psm.entity;

import jakarta.persistence.*;

/**
 * Projet réalisé pour un client. Relation Many-to-One vers Client (colonne id_client).
 */
@Entity
@Table(name = "projets")
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String libelle;

    /** Budget en dinars tunisiens (TND) */
    @Column(nullable = false)
    private Double budget;

    /** Durée en mois */
    @Column(nullable = false)
    private Integer duree;

    @Column(length = 2000)
    private String description;

    /** Statut du projet (bonus : filtres + statistiques) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutProjet statut = StatutProjet.EN_ATTENTE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_client", nullable = false)
    private Client client;

    public Projet() {}

    public Projet(String libelle, Double budget, Integer duree, String description, StatutProjet statut, Client client) {
        this.libelle = libelle;
        this.budget = budget;
        this.duree = duree;
        this.description = description;
        this.statut = statut;
        this.client = client;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }
    public Integer getDuree() { return duree; }
    public void setDuree(Integer duree) { this.duree = duree; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public StatutProjet getStatut() { return statut; }
    public void setStatut(StatutProjet statut) { this.statut = statut; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
}
