package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.ProjetDto;
import com.smartsoft.psm.dto.StatsDto;
import com.smartsoft.psm.dto.StatsDto.TopClient;
import com.smartsoft.psm.entity.StatutProjet;
import com.smartsoft.psm.repository.ClientRepository;
import com.smartsoft.psm.repository.ProjetRepository;
import com.smartsoft.psm.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final ClientRepository clientRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;

    public StatsService(ClientRepository clientRepository, ProjetRepository projetRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.projetRepository = projetRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public StatsDto getStats() {
        Map<String, Long> parStatut = new LinkedHashMap<>();
        for (StatutProjet s : StatutProjet.values()) {
            parStatut.put(s.name(), projetRepository.countByStatut(s));
        }
        List<TopClient> top = projetRepository.topClientsParBudget(PageRequest.of(0, 5)).stream()
                .map(r -> new TopClient((String) r[0], ((Number) r[1]).doubleValue()))
                .toList();
        List<ProjetDto> derniers = projetRepository.findTop5ByOrderByIdDesc().stream().map(ProjetDto::from).toList();
        Double budget = projetRepository.sumBudget();

        return new StatsDto(clientRepository.count(), projetRepository.count(), budget == null ? 0 : budget,
                userRepository.count(), userRepository.countByEnabledFalse(), parStatut, top, derniers);
    }
}
