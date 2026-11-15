package com.smartsoft.psm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Préférences d'affichage d'un utilisateur, enregistrées sur son compte
 * (il les retrouve sur n'importe quel ordinateur).
 *  - theme     : "light" ou "dark"
 *  - dashboard : blocs du tableau de bord, dans l'ordre d'affichage, visibles ou masqués
 */
public record PreferencesDto(
        @Pattern(regexp = "light|dark", message = "Thème inconnu (light ou dark)") String theme,
        @Size(max = 20) List<@Valid Widget> dashboard) {

    public record Widget(
            @NotBlank @Size(max = 30) @Pattern(regexp = "[a-z-]+", message = "Identifiant de bloc invalide") String id,
            boolean visible) {}
}
