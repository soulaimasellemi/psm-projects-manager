package com.smartsoft.psm.config;

import com.smartsoft.psm.entity.*;
import com.smartsoft.psm.repository.ClientRepository;
import com.smartsoft.psm.repository.ProjetRepository;
import com.smartsoft.psm.repository.RoleRepository;
import com.smartsoft.psm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.smartsoft.psm.entity.StatutProjet.*;

/**
 * Au premier démarrage : crée les rôles, les comptes de démonstration
 * et un jeu de données réaliste (agence digitale).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ProjetRepository projetRepository;
    private final PasswordEncoder encoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           ClientRepository clientRepository, ProjetRepository projetRepository,
                           PasswordEncoder encoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.projetRepository = projetRepository;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Role admin = roleRepository.findByLibelle(Role.ADMIN).orElseGet(() -> roleRepository.save(new Role(Role.ADMIN)));
        Role collab = roleRepository.findByLibelle(Role.COLLABORATEUR).orElseGet(() -> roleRepository.save(new Role(Role.COLLABORATEUR)));

        if (userRepository.count() == 0) {
            User a = new User("admin@smartsoft.tn", encoder.encode("Admin@123"), "+216 71 000 000", "Ben Salah", "Amine", true);
            a.getRoles().add(admin);
            User c = new User("collab@smartsoft.tn", encoder.encode("Collab@123"), "+216 98 111 222", "Trabelsi", "Sarra", true);
            c.getRoles().add(collab);
            User p = new User("yassine.gharbi@smartsoft.tn", encoder.encode("Yassine@123"), "+216 55 333 444", "Gharbi", "Yassine", false);
            p.getRoles().add(collab);
            userRepository.save(a);
            userRepository.save(c);
            userRepository.save(p);
            log.info("Comptes de démo créés : admin@smartsoft.tn / Admin@123  |  collab@smartsoft.tn / Collab@123");
        }

        if (clientRepository.count() == 0) {
            Client carthage = client("Carthage Bank", "+216 71 234 567", "contact@carthagebank.tn", "Avenue Habib Bourguiba, Tunis", "Banque & Finance");
            Client medina = client("Medina Store", "+216 73 456 789", "hello@medinastore.tn", "Rue de la Kasbah, Sousse", "E-commerce");
            Client sahel = client("Sahel Logistique", "+216 74 222 333", "info@sahel-logistique.tn", "Zone industrielle, Sfax", "Transport & Logistique");
            Client djerba = client("Djerba Travel", "+216 75 650 100", "booking@djerbatravel.tn", "Houmt Souk, Djerba", "Tourisme");
            Client atlas = client("Atlas Santé", "+216 71 890 120", "direction@atlas-sante.tn", "Les Berges du Lac, Tunis", "Santé");
            Client olivia = client("Olivia Agro", "+216 78 300 400", "export@olivia-agro.tn", "Route de Gabès, Sfax", "Agroalimentaire");

            projet("Application mobile bancaire", 85000, 8, "App iOS/Android : consultation des comptes, virements et notifications.", EN_COURS, carthage);
            projet("Portail client en ligne", 42000, 5, "Espace client web sécurisé avec authentification forte.", TERMINE, carthage);
            projet("Boutique e-commerce", 30000, 4, "Site marchand avec paiement en ligne et gestion des stocks.", EN_COURS, medina);
            projet("Chatbot service client", 20000, 3, "Assistant conversationnel pour le support et le suivi de commandes.", EN_ATTENTE, medina);
            projet("ERP gestion de flotte", 65000, 9, "Suivi des camions, planification des tournées et facturation.", EN_ATTENTE, sahel);
            projet("Tableau de bord BI", 25000, 3, "Indicateurs de performance logistique en temps réel.", TERMINE, sahel);
            projet("Site vitrine & réservation", 18000, 3, "Site multilingue avec moteur de réservation d'excursions.", EN_COURS, djerba);
            projet("Plateforme de télémédecine", 120000, 12, "Téléconsultation, dossier patient et prise de rendez-vous.", EN_COURS, atlas);
            projet("Refonte du site web", 12000, 2, "Nouveau design responsive et optimisation SEO.", TERMINE, olivia);
            projet("Application de traçabilité", 38000, 6, "Suivi des lots d'huile d'olive de la récolte à l'export.", EN_ATTENTE, olivia);
            log.info("Données de démonstration créées (6 clients, 10 projets)");
        }
    }

    private Client client(String nom, String tel, String email, String adresse, String secteur) {
        return clientRepository.save(new Client(nom, tel, email, adresse, secteur));
    }

    private void projet(String libelle, double budget, int duree, String desc, StatutProjet statut, Client c) {
        Projet p = projetRepository.save(new Projet(libelle, budget, duree, desc, statut, c));
        c.getProjets().add(p);
    }
}
