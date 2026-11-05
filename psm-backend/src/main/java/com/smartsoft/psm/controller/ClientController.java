package com.smartsoft.psm.controller;

import com.smartsoft.psm.dto.ClientDto;
import com.smartsoft.psm.dto.ClientDto.ClientOption;
import com.smartsoft.psm.dto.ClientDto.ClientRequest;
import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET    : ADMIN + COLLABORATEUR
 * POST   : ADMIN + COLLABORATEUR
 * PUT    : ADMIN
 * DELETE : ADMIN
 * (règles appliquées dans SecurityConfig)
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public PageResponse<ClientDto> list(@RequestParam(defaultValue = "") String q,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return clientService.findAll(q, page, size);
    }

    /** Liste courte (id, nom) pour les listes déroulantes */
    @GetMapping("/all")
    public List<ClientOption> options() {
        return clientService.findOptions();
    }

    @GetMapping("/{id}")
    public ClientDto get(@PathVariable Long id) {
        return clientService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ClientDto> create(@Valid @RequestBody ClientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(req));
    }

    @PutMapping("/{id}")
    public ClientDto update(@PathVariable Long id, @Valid @RequestBody ClientRequest req) {
        return clientService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
