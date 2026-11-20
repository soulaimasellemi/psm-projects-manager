package com.smartsoft.psm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Projet réalisé pour un client. Relation Many-to-One vers Client (colonne id_client)
 * et Many-to-Many vers User (collaborateurs affectés, relation "gérer" du diagramme de classes).
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

    /** Suivi : date de début et date de fin prévue (facultatives) */
    private LocalDate dateDebut;
    private LocalDate dateFinPrevue;

    /** Avancement en % (0 à 100) */
    private Integer avancement = 0;

    /** Collaborateurs affectés au projet */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "projet_membres",
            joinColumns = @JoinColumn(name = "projet_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> membres = new HashSet<>();

    /** Tâches du tableau Kanban (supprimées avec le projet) */
    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @OrderBy("ordre ASC, id ASC")
    private List<Tache> taches = new ArrayList<>();

    public Projet() {}

    public Projet(String libelle, Double budget, Integer duree, String description, StatutProjet statut, Client client) {
        this.libelle = libelle;
        this.budget = budget;
        this.duree = duree;
        this.description = description;
        this.statut = statut;
        this.client = client;
    }

    /** En retard : date de fin prévue dépassée alors que le projet n'est pas terminé */
    public boolean isEnRetard() {
        return dateFinPrevue != null && statut != StatutProjet.TERMINE && dateFinPrevue.isBefore(LocalDate.now());
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
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFinPrevue() { return dateFinPrevue; }
    public void setDateFinPrevue(LocalDate dateFinPrevue) { this.dateFinPrevue = dateFinPrevue; }
    public int getAvancement() { return avancement == null ? 0 : avancement; }
    public void setAvancement(Integer avancement) { this.avancement = avancement; }
    public Set<User> getMembres() { return membres; }
    public void setMembres(Set<User> membres) { this.membres = membres; }
    public List<Tache> getTaches() { return taches; }

    /**
     * Avancement calculé à partir des tâches : % de tâches terminées.
     * Sans tâche, l'avancement saisi manuellement est conservé.
     */
    public void recalculerAvancement() {
        if (statut == StatutProjet.TERMINE) {
            avancement = 100;
        } else if (!taches.isEmpty()) {
            long terminees = taches.stream().filter(t -> t.getStatut() == Tache.Statut.TERMINEE).count();
            avancement = (int) Math.round(terminees * 100.0 / taches.size());
        }
    }
}
