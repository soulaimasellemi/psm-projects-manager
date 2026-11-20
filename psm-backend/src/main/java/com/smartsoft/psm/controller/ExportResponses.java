package com.smartsoft.psm.controller;

import com.smartsoft.psm.service.ExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Construit la réponse HTTP d'un fichier exporté (téléchargement avec son nom). */
final class ExportResponses {

    private ExportResponses() {}

    static ResponseEntity<byte[]> telechargement(ExportService.Fichier f) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(f.nom(), StandardCharsets.UTF_8).build().toString())
                .body(f.contenu());
    }
}
