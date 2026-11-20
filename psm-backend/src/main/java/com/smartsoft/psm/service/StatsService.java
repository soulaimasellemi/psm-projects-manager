package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.ProjetDto;
import com.smartsoft.psm.dto.StatsDto;
import com.smartsoft.psm.dto.SanteDto;
import com.smartsoft.psm.dto.StatsDto.Avancement;
import com.smartsoft.psm.dto.StatsDto.ProjetRisque;
import com.smartsoft.psm.dto.StatsDto.TopClient;
import com.smartsoft.psm.entity.StatutProjet;
import com.smartsoft.psm.entity.User;
import com.smartsoft.psm.repository.ClientRepository;
import com.smartsoft.psm.repository.ProjetRepository;
import com.smartsoft.psm.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
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

    /** @param current utilisateur connecté (pour le compteur "mes projets") */
    @Transactional(readOnly = true)
    public StatsDto getStats(User current) {
        Map<String, Long> parStatut = new LinkedHashMap<>();
        for (StatutProjet s : StatutProjet.values()) {
            parStatut.put(s.name(), projetRepository.countByStatut(s));
        }
        List<TopClient> top = projetRepository.topClientsParBudget(PageRequest.of(0, 5)).stream()
                .map(r -> new TopClient((String) r[0], ((Number) r[1]).doubleValue()))
                .toList();
        List<ProjetDto> derniers = projetRepository.findTop5ByOrderByIdDesc().stream().map(ProjetDto::from).toList();
        List<Avancement> avancements = projetRepository.findTop8ByStatutOrderByAvancementDesc(StatutProjet.EN_COURS).stream()
                .map(p -> new Avancement(p.getId(), p.getLibelle(), p.getAvancement(), p.isEnRetard()))
                .toList();
        Double budget = projetRepository.sumBudget();
        Double moyenne = projetRepository.avancementMoyenEnCours();
        long mesProjets = current == null ? 0 : projetRepository.countByMembresContaining(current);

        // Santé des projets non terminés : moyenne, compteurs par niveau, et les 5 plus à risque
        List<ProjetRisque> risques = projetRepository.findByStatutNot(StatutProjet.TERMINE).stream()
                .map(p -> {
                    SanteDto s = SanteDto.calculer(p);
                    return new ProjetRisque(p.getId(), p.getLibelle(), p.getClient().getNom(), s.score(), s.niveau(), s.raisons());
                })
                .sorted(Comparator.comparingInt(ProjetRisque::score))
                .toList();
        int santeMoyenne = (int) Math.round(risques.stream().mapToInt(ProjetRisque::score).average().orElse(100));
        long critiques = risques.stream().filter(r -> r.niveau() == SanteDto.Niveau.CRITIQUE).count();
        long aSurveiller = risques.stream().filter(r -> r.niveau() == SanteDto.Niveau.A_SURVEILLER).count();
        List<ProjetRisque> topRisques = risques.stream().filter(r -> r.niveau() != SanteDto.Niveau.SAIN).limit(5).toList();

        return new StatsDto(clientRepository.count(), projetRepository.count(), budget == null ? 0 : budget,
                userRepository.count(), userRepository.countByEnabledFalse(), parStatut, top, derniers,
                projetRepository.countEnRetard(LocalDate.now()),
                moyenne == null ? 0 : (int) Math.round(moyenne), avancements, mesProjets,
                santeMoyenne, critiques, aSurveiller, topRisques);
    }
}
