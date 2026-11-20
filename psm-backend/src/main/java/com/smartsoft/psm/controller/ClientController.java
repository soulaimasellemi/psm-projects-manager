package com.smartsoft.psm.controller;

import com.smartsoft.psm.dto.ClientDto;
import com.smartsoft.psm.dto.ClientDto.ClientOption;
import com.smartsoft.psm.dto.ClientDto.ClientRequest;
import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.service.ClientService;
import com.smartsoft.psm.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import java.util.List;

/**
 * GET    : ADMIN + COLLABORATEUR
 * POST   : ADMIN + COLLABORATEUR
 * PUT    : ADMIN
 * DELETE : ADMIN
 * (règles appliquées dans SecurityConfig)
 *
 * Fichier joint : POST /{id}/fichier (upload), GET /{id}/fichier (download), DELETE /{id}/fichier (ADMIN)
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

    /** Export Excel ou PDF : /api/clients/export?format=xlsx|pdf&q=... */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "xlsx") String format,
                                         @RequestParam(defaultValue = "") String q) {
        return ExportResponses.telechargement(clientService.exporter(q, ExportService.Format.parse(format)));
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
    public ResponseEntity<ClientDto> create(@Valid @RequestBody ClientRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(req, auth.getName()));
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

    // ---------- Fichier joint ----------

    /** Upload : formulaire multipart avec un champ "file" */
    @PostMapping(value = "/{id}/fichier", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClientDto uploadFichier(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return clientService.uploadFichier(id, file);
    }

    /** Download : renvoie le fichier avec son nom d'origine */
    @GetMapping("/{id}/fichier")
    public ResponseEntity<Resource> downloadFichier(@PathVariable Long id) {
        var f = clientService.downloadFichier(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.type()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(f.nom(), StandardCharsets.UTF_8).build().toString())
                .body(f.resource());
    }

    /** Supprime le fichier joint (ADMIN) */
    @DeleteMapping("/{id}/fichier")
    public ResponseEntity<Void> deleteFichier(@PathVariable Long id) {
        clientService.deleteFichier(id);
        return ResponseEntity.noContent().build();
    }
}
