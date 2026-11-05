package com.smartsoft.psm.controller;

import com.smartsoft.psm.dto.AuthDtos.AuthResponse;
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

    /** POST /api/auth/register -> compte créé inactif + email envoyé à l'admin */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(
                "Inscription enregistrée. Votre compte sera actif dès sa validation par l'administrateur."));
    }
}
