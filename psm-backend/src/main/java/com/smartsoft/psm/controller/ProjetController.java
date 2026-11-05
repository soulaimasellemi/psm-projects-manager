package com.smartsoft.psm.controller;

import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.dto.ProjetDto;
import com.smartsoft.psm.dto.ProjetDto.ProjetRequest;
import com.smartsoft.psm.entity.StatutProjet;
import com.smartsoft.psm.service.ProjetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Mêmes règles d'accès que les clients (voir SecurityConfig). */
@RestController
@RequestMapping("/api/projets")
public class ProjetController {

    private final ProjetService projetService;

    public ProjetController(ProjetService projetService) {
        this.projetService = projetService;
    }

    /** Filtres optionnels : q (texte), statut, clientId */
    @GetMapping
    public PageResponse<ProjetDto> list(@RequestParam(defaultValue = "") String q,
                                        @RequestParam(required = false) StatutProjet statut,
                                        @RequestParam(required = false) Long clientId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return projetService.findAll(q, statut, clientId, page, size);
    }

    @GetMapping("/{id}")
    public ProjetDto get(@PathVariable Long id) {
        return projetService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProjetDto> create(@Valid @RequestBody ProjetRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetService.create(req));
    }

    @PutMapping("/{id}")
    public ProjetDto update(@PathVariable Long id, @Valid @RequestBody ProjetRequest req) {
        return projetService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
