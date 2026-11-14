package com.smartsoft.psm.service;

import com.smartsoft.psm.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Stockage des fichiers sur le disque (dossier psm.upload.dir).
 * Le fichier est enregistré sous un nom aléatoire (UUID) : le nom envoyé par
 * l'utilisateur n'est jamais utilisé comme chemin (évite les attaques "../").
 */
@Service
public class FichierService {

    private static final Logger log = LoggerFactory.getLogger(FichierService.class);

    /** Types acceptés : extension -> type MIME */
    private static final Map<String, String> TYPES_AUTORISES = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "webp", "image/webp",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final Path dossier;

    public FichierService(@Value("${psm.upload.dir}") String dossier) throws IOException {
        this.dossier = Path.of(dossier).toAbsolutePath().normalize();
        Files.createDirectories(this.dossier);
        log.info("Dossier des fichiers : {}", this.dossier);
    }

    /** Enregistre le fichier et renvoie le nom sous lequel il est stocké. */
    public String enregistrer(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Aucun fichier envoyé");
        String extension = extension(file.getOriginalFilename());
        if (!TYPES_AUTORISES.containsKey(extension)) {
            throw ApiException.badRequest("Type de fichier non autorisé. Acceptés : " + String.join(", ", TYPES_AUTORISES.keySet()));
        }
        String nomStocke = UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dossier.resolve(nomStocke), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer le fichier", e);
        }
        return nomStocke;
    }

    public Resource charger(String nomStocke) {
        try {
            Path chemin = dossier.resolve(nomStocke).normalize();
            Resource resource = new UrlResource(chemin.toUri());
            if (!chemin.startsWith(dossier) || !resource.exists()) {
                throw ApiException.notFound("Fichier introuvable sur le serveur");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw ApiException.notFound("Fichier introuvable sur le serveur");
        }
    }

    public void supprimer(String nomStocke) {
        if (nomStocke == null) return;
        try {
            Files.deleteIfExists(dossier.resolve(nomStocke).normalize());
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier {} ({})", nomStocke, e.getMessage());
        }
    }

    /** Type MIME déduit de l'extension (ne fait pas confiance au type envoyé par le client). */
    public String typeMime(String nomFichier) {
        return TYPES_AUTORISES.getOrDefault(extension(nomFichier), "application/octet-stream");
    }

    private static String extension(String nom) {
        String ext = StringUtils.getFilenameExtension(nom);
        return ext == null ? "" : ext.toLowerCase();
    }
}
