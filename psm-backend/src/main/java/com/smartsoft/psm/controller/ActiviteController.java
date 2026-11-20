package com.smartsoft.psm.controller;

import com.smartsoft.psm.dto.ActiviteDto;
import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.entity.Activite.Action;
import com.smartsoft.psm.entity.Activite.Cible;
import com.smartsoft.psm.service.ActiviteService;
import org.springframework.web.bind.annotation.*;

/** Historique des actions : ADMIN uniquement (voir SecurityConfig). */
@RestController
@RequestMapping("/api/activites")
public class ActiviteController {

    private final ActiviteService activiteService;

    public ActiviteController(ActiviteService activiteService) {
        this.activiteService = activiteService;
    }

    /** Filtres optionnels : q (auteur ou détail), cible (CLIENT, PROJET, UTILISATEUR), action */
    @GetMapping
    public PageResponse<ActiviteDto> list(@RequestParam(defaultValue = "") String q,
                                          @RequestParam(required = false) Cible cible,
                                          @RequestParam(required = false) Action action,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "15") int size) {
        return activiteService.findAll(q, cible, action, page, size);
    }
}
