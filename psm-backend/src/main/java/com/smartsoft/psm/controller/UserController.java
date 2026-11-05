package com.smartsoft.psm.controller;

import com.smartsoft.psm.dto.AuthDtos.MessageResponse;
import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.dto.UserDto;
import com.smartsoft.psm.dto.UserDto.*;
import com.smartsoft.psm.entity.User;
import com.smartsoft.psm.service.UserService;
import jakarta.validation.Valid;
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

    @PutMapping("/me/password")
    public MessageResponse changePassword(@AuthenticationPrincipal User current, @Valid @RequestBody PasswordRequest req) {
        userService.changePassword(current.getId(), req);
        return new MessageResponse("Mot de passe modifié avec succès.");
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
