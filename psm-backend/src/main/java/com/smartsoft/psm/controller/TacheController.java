package com.smartsoft.psm.controller;

import com.smartsoft.psm.dto.TacheDto;
import com.smartsoft.psm.dto.TacheDto.DeplacementRequest;
import com.smartsoft.psm.dto.TacheDto.TacheRequest;
import com.smartsoft.psm.entity.User;
import com.smartsoft.psm.service.TacheService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tâches du Kanban.
 *  GET  /api/projets/{id}/taches       : ADMIN + COLLABORATEUR
 *  POST /api/projets/{id}/taches       : ADMIN + membres du projet
 *  PUT  /api/taches/{id}               : ADMIN + membres du projet (modifier)
 *  PUT  /api/taches/{id}/deplacer      : ADMIN + membres du projet (glisser-déposer)
 *  DELETE /api/taches/{id}             : ADMIN
 */
@RestController
@RequestMapping("/api")
public class TacheController {

    private final TacheService tacheService;

    public TacheController(TacheService tacheService) {
        this.tacheService = tacheService;
    }

    @GetMapping("/projets/{projetId}/taches")
    public List<TacheDto> list(@PathVariable Long projetId) {
        return tacheService.findByProjet(projetId);
    }

    @PostMapping("/projets/{projetId}/taches")
    public ResponseEntity<TacheDto> create(@PathVariable Long projetId, @Valid @RequestBody TacheRequest req,
                                           @AuthenticationPrincipal User current) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tacheService.create(projetId, req, current));
    }

    @PutMapping("/taches/{id}")
    public TacheDto update(@PathVariable Long id, @Valid @RequestBody TacheRequest req,
                           @AuthenticationPrincipal User current) {
        return tacheService.update(id, req, current);
    }

    @PutMapping("/taches/{id}/deplacer")
    public TacheDto deplacer(@PathVariable Long id, @Valid @RequestBody DeplacementRequest req,
                             @AuthenticationPrincipal User current) {
        return tacheService.deplacer(id, req, current);
    }

    @DeleteMapping("/taches/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tacheService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
