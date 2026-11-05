package com.smartsoft.psm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO liés à l'authentification (regroupés dans un seul fichier). */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
            @NotBlank(message = "Le mot de passe est obligatoire") String password) {}

    public record RegisterRequest(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères") String password,
            @Size(max = 30) String tel,
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 80) String nom,
            @NotBlank(message = "Le prénom est obligatoire") @Size(max = 80) String prenom) {}

    public record AuthResponse(String token, UserDto user) {}

    public record MessageResponse(String message) {}
}
