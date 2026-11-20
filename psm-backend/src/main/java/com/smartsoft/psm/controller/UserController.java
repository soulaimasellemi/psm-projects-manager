package com.smartsoft.psm.controller;

import com.smartsoft.psm.dto.AuthDtos.MessageResponse;
import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.dto.PreferencesDto;
import com.smartsoft.psm.dto.ProjetDto;
import com.smartsoft.psm.dto.UserDto;
import java.util.List;
import com.smartsoft.psm.dto.UserDto.*;
import com.smartsoft.psm.entity.User;
import com.smartsoft.psm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * /api/users/me/**  : tout utilisateur connecté (son profil)
 * /api/users/**     : ADMIN uniquement
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ---------- Profil ----------

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal User current) {
        return userService.me(current.getId());
    }

    @PutMapping("/me")
    public UserDto updateMe(@AuthenticationPrincipal User current, @Valid @RequestBody ProfilRequest req) {
        return userService.updateProfil(current.getId(), req);
    }

    /** Thème (clair / sombre) et disposition du dashboard, enregistrés sur le compte */
    @PutMapping("/me/preferences")
    public UserDto updatePreferences(@AuthenticationPrincipal User current, @Valid @RequestBody PreferencesDto req) {
        return userService.updatePreferences(current.getId(), req);
    }

    @PutMapping("/me/password")
    public MessageResponse changePassword(@AuthenticationPrincipal User current, @Valid @RequestBody PasswordRequest req) {
        userService.changePassword(current.getId(), req);
        return new MessageResponse("Mot de passe modifié avec succès.");
    }

    // ---------- Collaborateurs (ADMIN + COLLABORATEUR) ----------

    /** Comptes actifs (id, nom complet, email) pour affecter des collaborateurs à un projet */
    @GetMapping("/options")
    public List<ProjetDto.Membre> options() {
        return userService.options();
    }

    // ---------- Administration ----------

    /** enabled=false -> uniquement les comptes en attente de validation */
    @GetMapping
    public PageResponse<UserDto> list(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(required = false) Boolean enabled,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return userService.findAll(q, enabled, page, size);
    }

    /** Créer un compte (ADMIN) : actif immédiatement, rôles au choix (ex. ["ADMIN"]) */
    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(req));
    }

    /** Activer / désactiver un compte (body : { "enabled": true }) */
    @PutMapping("/{id}/statut")
    public UserDto changerStatut(@PathVariable Long id, @Valid @RequestBody StatutRequest req,
                                 @AuthenticationPrincipal User current) {
        return userService.changerStatut(id, req.enabled(), current.getId());
    }

    /** Modifier les rôles (body : { "roles": ["ADMIN", "COLLABORATEUR"] }) */
    @PutMapping("/{id}/roles")
    public UserDto changerRoles(@PathVariable Long id, @Valid @RequestBody RolesRequest req,
                                @AuthenticationPrincipal User current) {
        return userService.changerRoles(id, req.roles(), current.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User current) {
        userService.delete(id, current.getId());
        return ResponseEntity.noContent().build();
    }
}
