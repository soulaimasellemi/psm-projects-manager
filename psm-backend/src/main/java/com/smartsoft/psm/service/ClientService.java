package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.ClientDto;
import com.smartsoft.psm.dto.ClientDto.ClientOption;
import com.smartsoft.psm.dto.ClientDto.ClientRequest;
import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.entity.Client;
import com.smartsoft.psm.exception.ApiException;
import com.smartsoft.psm.repository.ClientRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final FichierService fichierService;
    private final EmailService emailService;

    /** Fichier prêt à être téléchargé */
    public record FichierClient(Resource resource, String nom, String type) {}

    public ClientService(ClientRepository clientRepository, FichierService fichierService, EmailService emailService) {
        this.clientRepository = clientRepository;
        this.fichierService = fichierService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientDto> findAll(String q, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending());
        return PageResponse.of(clientRepository.search(q == null ? "" : q.trim(), pageable).map(ClientDto::from));
    }

    @Transactional(readOnly = true)
    public List<ClientOption> findOptions() {
        return clientRepository.findAllByOrderByNomAsc().stream().map(c -> new ClientOption(c.getId(), c.getNom())).toList();
    }

    @Transactional(readOnly = true)
    public ClientDto findById(Long id) {
        return ClientDto.from(getEntity(id));
    }

    /** Crée le client puis envoie un email de notification à l'admin. */
    public ClientDto create(ClientRequest req, String creePar) {
        Client c = new Client();
        apply(c, req);
        ClientDto dto = ClientDto.from(clientRepository.save(c));
        emailService.notifierAdminNouveauClient(dto, creePar);
        return dto;
    }

    public ClientDto update(Long id, ClientRequest req) {
        Client c = getEntity(id);
        apply(c, req);
        return ClientDto.from(c);
    }

    public void delete(Long id) {
        Client c = getEntity(id);
        clientRepository.delete(c); // supprime aussi ses projets (cascade)
        fichierService.supprimer(c.getFichierStocke());
    }

    // ---------- Fichier joint (upload / download) ----------

    /** Envoie (ou remplace) le fichier joint du client. */
    public ClientDto uploadFichier(Long id, MultipartFile file) {
        Client c = getEntity(id);
        String ancien = c.getFichierStocke();
        String nomOrigine = file.getOriginalFilename() == null ? "fichier" : file.getOriginalFilename();
        c.setFichierStocke(fichierService.enregistrer(file));
        c.setFichierNom(nomOrigine.length() > 255 ? nomOrigine.substring(nomOrigine.length() - 255) : nomOrigine);
        c.setFichierType(fichierService.typeMime(nomOrigine));
        fichierService.supprimer(ancien);
        return ClientDto.from(c);
    }

    @Transactional(readOnly = true)
    public FichierClient downloadFichier(Long id) {
        Client c = getEntity(id);
        if (c.getFichierStocke() == null) throw ApiException.notFound("Ce client n'a pas de fichier joint");
        return new FichierClient(fichierService.charger(c.getFichierStocke()), c.getFichierNom(), c.getFichierType());
    }

    public void deleteFichier(Long id) {
        Client c = getEntity(id);
        if (c.getFichierStocke() == null) throw ApiException.notFound("Ce client n'a pas de fichier joint");
        fichierService.supprimer(c.getFichierStocke());
        c.setFichierStocke(null);
        c.setFichierNom(null);
        c.setFichierType(null);
    }

    Client getEntity(Long id) {
        return clientRepository.findById(id).orElseThrow(() -> ApiException.notFound("Client introuvable (id=" + id + ")"));
    }

    private static void apply(Client c, ClientRequest req) {
        c.setNom(req.nom().trim());
        c.setTel(blankToNull(req.tel()));
        c.setEmail(blankToNull(req.email()));
        c.setAdresse(blankToNull(req.adresse()));
        c.setSecteur(blankToNull(req.secteur()));
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
