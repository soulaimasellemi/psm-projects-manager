package com.smartsoft.psm.dto;

import com.smartsoft.psm.entity.Role;
import com.smartsoft.psm.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/** Représentation d'un utilisateur renvoyée par l'API (jamais le mot de passe). */
public record UserDto(Long id, String email, String tel, String nom, String prenom,
                      boolean enabled, Set<String> roles, LocalDateTime dateCreation) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getTel(), u.getNom(), u.getPrenom(),
                u.isEnabled(),
                u.getRoles().stream().map(Role::getLibelle).collect(Collectors.toCollection(java.util.TreeSet::new)),
                u.getDateCreation());
    }

    public record ProfilRequest(
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 80) String nom,
            @NotBlank(message = "Le prénom est obligatoire") @Size(max = 80) String prenom,
            @Size(max = 30) String tel) {}

    public record PasswordRequest(
            @NotBlank(message = "L'ancien mot de passe est obligatoire") String ancien,
            @NotBlank @Size(min = 6, message = "Le nouveau mot de passe doit contenir au moins 6 caractères") String nouveau) {}

    public record StatutRequest(@NotNull Boolean enabled) {}

    public record RolesRequest(@NotEmpty(message = "Au moins un rôle est requis") Set<String> roles) {}
}
