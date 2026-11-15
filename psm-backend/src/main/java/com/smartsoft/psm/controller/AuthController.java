package com.smartsoft.psm.controller;

import com.smartsoft.psm.dto.AuthDtos.AuthResponse;
import com.smartsoft.psm.dto.AuthDtos.ForgotPasswordRequest;
import com.smartsoft.psm.dto.AuthDtos.ResetPasswordRequest;
import com.smartsoft.psm.dto.AuthDtos.LoginRequest;
import com.smartsoft.psm.dto.AuthDtos.MessageResponse;
import com.smartsoft.psm.dto.AuthDtos.RegisterRequest;
import com.smartsoft.psm.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/login -> { token, user } */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** POST /api/auth/forgot-password -> envoie un lien de réinitialisation (réponse identique si l'email est inconnu) */
    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.demanderReinitialisation(req.email());
        return new MessageResponse(
                "Si un compte actif existe avec cet email, un lien de réinitialisation vient de lui être envoyé (valable 30 minutes).");
    }

    /** POST /api/auth/reset-password -> nouveau mot de passe avec le jeton reçu par email */
    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.reinitialiser(req.token(), req.password());
        return new MessageResponse("Votre mot de passe a été modifié. Vous pouvez maintenant vous connecter.");
    }

    /** POST /api/auth/register -> compte créé inactif + email envoyé à l'admin */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(
                "Inscription enregistrée. Votre compte sera actif dès sa validation par l'administrateur."));
    }
}
