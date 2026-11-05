package com.smartsoft.psm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Client de l'agence. Relation One-to-Many avec Projet :
 * supprimer un client supprime ses projets.
 */
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nom;

    @Column(length = 30)
    private String tel;

    @Column(length = 120)
    private String email;

    @Column(length = 255)
    private String adresse;

    /** Secteur d'activité (bonus thème agence digitale) */
    @Column(length = 60)
    private String secteur;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<Projet> projets = new ArrayList<>();

    public Client() {}

    public Client(String nom, String tel, String email, String adresse, String secteur) {
        this.nom = nom;
        this.tel = tel;
        this.email = email;
        this.adresse = adresse;
        this.secteur = secteur;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getSecteur() { return secteur; }
    public void setSecteur(String secteur) { this.secteur = secteur; }
    public List<Projet> getProjets() { return projets; }
    public void setProjets(List<Projet> projets) { this.projets = projets; }
}
