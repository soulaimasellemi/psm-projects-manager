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

    /** Mot de passe oublié : étape 1, demande du lien par email */
    public record ForgotPasswordRequest(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email) {}

    /** Mot de passe oublié : étape 2, nouveau mot de passe avec le jeton reçu par email */
    public record ResetPasswordRequest(
            @NotBlank(message = "Le lien de réinitialisation est invalide") String token,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password) {}

    /** Validation d'une inscription avec le jeton du lien reçu par l'admin */
    public record ValidationRequest(@NotBlank(message = "Le lien de validation est invalide") String token) {}

    public record AuthResponse(String token, UserDto user) {}

    public record MessageResponse(String message) {}
}
