package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.dto.ProjetDto;
import com.smartsoft.psm.dto.ProjetDto.ProjetRequest;
import com.smartsoft.psm.entity.Projet;
import com.smartsoft.psm.entity.StatutProjet;
import com.smartsoft.psm.exception.ApiException;
import com.smartsoft.psm.repository.ProjetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final ClientService clientService;

    public ProjetService(ProjetRepository projetRepository, ClientService clientService) {
        this.projetRepository = projetRepository;
        this.clientService = clientService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjetDto> findAll(String q, StatutProjet statut, Long clientId, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending());
        return PageResponse.of(projetRepository.search(q == null ? "" : q.trim(), statut, clientId, pageable)
                .map(ProjetDto::from));
    }

    @Transactional(readOnly = true)
    public ProjetDto findById(Long id) {
        return ProjetDto.from(getEntity(id));
    }

    public ProjetDto create(ProjetRequest req) {
        Projet p = new Projet();
        apply(p, req);
        return ProjetDto.from(projetRepository.save(p));
    }

    public ProjetDto update(Long id, ProjetRequest req) {
        Projet p = getEntity(id);
        apply(p, req);
        return ProjetDto.from(p);
    }

    public void delete(Long id) {
        Projet p = getEntity(id);
        p.getClient().getProjets().remove(p);
        projetRepository.delete(p);
    }

    private Projet getEntity(Long id) {
        return projetRepository.findById(id).orElseThrow(() -> ApiException.notFound("Projet introuvable (id=" + id + ")"));
    }

    private void apply(Projet p, ProjetRequest req) {
        p.setLibelle(req.libelle().trim());
        p.setBudget(req.budget());
        p.setDuree(req.duree());
        p.setDescription(ClientService.blankToNull(req.description()));
        p.setStatut(req.statut() == null ? StatutProjet.EN_ATTENTE : req.statut());
        p.setClient(clientService.getEntity(req.idClient()));
    }
}
