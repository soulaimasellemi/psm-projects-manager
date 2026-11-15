package com.smartsoft.psm.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Utilisateur de l'application. Implémente UserDetails pour Spring Security.
 * Relation Many-to-Many avec Role (table de jointure user_roles).
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 30)
    private String tel;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(nullable = false, length = 80)
    private String prenom;

    /** false tant que l'admin n'a pas validé l'inscription */
    @Column(nullable = false)
    private boolean enabled = false;

    private LocalDateTime dateCreation;

    /** Mot de passe oublié : empreinte SHA-256 du jeton envoyé par email (jamais le jeton en clair) */
    @Column(length = 64)
    private String resetTokenHash;

    private LocalDateTime resetTokenExpiration;

    /** Validation de l'inscription en un clic depuis l'email de l'admin (empreinte SHA-256 du jeton) */
    @Column(length = 64)
    private String validationTokenHash;

    private LocalDateTime validationTokenExpiration;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }

    public User() {}

    public User(String email, String password, String tel, String nom, String prenom, boolean enabled) {
        this.email = email;
        this.password = password;
        this.tel = tel;
        this.nom = nom;
        this.prenom = prenom;
        this.enabled = enabled;
    }

    public boolean hasRole(String libelle) {
        return roles.stream().anyMatch(r -> r.getLibelle().equals(libelle));
    }

    // ----- UserDetails -----
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.getLibelle())).toList();
    }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    @Override public boolean isEnabled() { return enabled; }

    // ----- Getters / Setters -----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public String getResetTokenHash() { return resetTokenHash; }
    public void setResetTokenHash(String resetTokenHash) { this.resetTokenHash = resetTokenHash; }
    public LocalDateTime getResetTokenExpiration() { return resetTokenExpiration; }
    public void setResetTokenExpiration(LocalDateTime resetTokenExpiration) { this.resetTokenExpiration = resetTokenExpiration; }

    public String getValidationTokenHash() { return validationTokenHash; }
    public LocalDateTime getValidationTokenExpiration() { return validationTokenExpiration; }

    public void definirJetonValidation(String hash, LocalDateTime expiration) {
        this.validationTokenHash = hash;
        this.validationTokenExpiration = expiration;
    }

    /** Le compte est activé (lien email ou page Utilisateurs) : le lien de validation ne sert plus */
    public void effacerJetonValidation() {
        definirJetonValidation(null, null);
    }

    public String getNomComplet() { return prenom + " " + nom; }
}
