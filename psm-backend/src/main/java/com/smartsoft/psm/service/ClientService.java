package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.ClientDto;
import com.smartsoft.psm.dto.ClientDto.ClientOption;
import com.smartsoft.psm.dto.ClientDto.ClientRequest;
import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.entity.Client;
import com.smartsoft.psm.exception.ApiException;
import com.smartsoft.psm.repository.ClientRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
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

    public ClientDto create(ClientRequest req) {
        Client c = new Client();
        apply(c, req);
        return ClientDto.from(clientRepository.save(c));
    }

    public ClientDto update(Long id, ClientRequest req) {
        Client c = getEntity(id);
        apply(c, req);
        return ClientDto.from(c);
    }

    public void delete(Long id) {
        clientRepository.delete(getEntity(id)); // supprime aussi ses projets (cascade)
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
