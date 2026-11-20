package com.smartsoft.psm.config;

import com.smartsoft.psm.entity.*;
import com.smartsoft.psm.repository.ClientRepository;
import com.smartsoft.psm.repository.ProjetRepository;
import com.smartsoft.psm.repository.RoleRepository;
import com.smartsoft.psm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smartsoft.psm.repository.TacheRepository;

import java.time.LocalDate;
import java.util.List;

import static com.smartsoft.psm.entity.StatutProjet.*;

/**
 * Au démarrage :
 *  1. crée les rôles ADMIN et COLLABORATEUR ;
 *  2. si psm.demo-data=true et que la base est vide : comptes de démonstration
 *     + jeu de données réaliste (agence digitale) ;
 *  3. crée l'administrateur initial défini dans psm.init-admin.* s'il n'existe pas encore.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${psm.demo-data:true}")
    private boolean demoData;

    @Value("${psm.init-admin.email:}")
    private String initAdminEmail;

    @Value("${psm.init-admin.password:}")
    private String initAdminPassword;

    @Value("${psm.init-admin.nom:Admin}")
    private String initAdminNom;

    @Value("${psm.init-admin.prenom:PSM}")
    private String initAdminPrenom;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ProjetRepository projetRepository;
    private final PasswordEncoder encoder;
    private final TacheRepository tacheRepository;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           ClientRepository clientRepository, ProjetRepository projetRepository,
                           PasswordEncoder encoder, TacheRepository tacheRepository) {
        this.tacheRepository = tacheRepository;
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

        boolean baseVide = userRepository.count() == 0;

        if (demoData && baseVide) {
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

        creerAdminInitial(admin);

        if (demoData && clientRepository.count() == 0) {
            Client carthage = client("Carthage Bank", "+216 71 234 567", "contact@carthagebank.tn", "Avenue Habib Bourguiba, Tunis", "Banque & Finance");
            Client medina = client("Medina Store", "+216 73 456 789", "hello@medinastore.tn", "Rue de la Kasbah, Sousse", "E-commerce");
            Client sahel = client("Sahel Logistique", "+216 74 222 333", "info@sahel-logistique.tn", "Zone industrielle, Sfax", "Transport & Logistique");
            Client djerba = client("Djerba Travel", "+216 75 650 100", "booking@djerbatravel.tn", "Houmt Souk, Djerba", "Tourisme");
            Client atlas = client("Atlas Santé", "+216 71 890 120", "direction@atlas-sante.tn", "Les Berges du Lac, Tunis", "Santé");
            Client olivia = client("Olivia Agro", "+216 78 300 400", "export@olivia-agro.tn", "Route de Gabès, Sfax", "Agroalimentaire");

            // Équipe de démonstration (affectation des collaborateurs aux projets)
            User amine = userRepository.findByEmail("admin@smartsoft.tn").orElse(null);
            User sarra = userRepository.findByEmail("collab@smartsoft.tn").orElse(null);

            // libellé, budget, durée, description, statut, client, début (il y a N mois), avancement %, équipe
            Projet mobile = projet("Application mobile bancaire", 85000, 8, "App iOS/Android : consultation des comptes, virements et notifications.", EN_COURS, carthage, 4, 55, amine, sarra);
            projet("Portail client en ligne", 42000, 5, "Espace client web sécurisé avec authentification forte.", TERMINE, carthage, 9, 100, amine);
            Projet boutique = projet("Boutique e-commerce", 30000, 4, "Site marchand avec paiement en ligne et gestion des stocks.", EN_COURS, medina, 5, 80, sarra); // en retard
            projet("Chatbot service client", 20000, 3, "Assistant conversationnel pour le support et le suivi de commandes.", EN_ATTENTE, medina, -1, 0);
            projet("ERP gestion de flotte", 65000, 9, "Suivi des camions, planification des tournées et facturation.", EN_ATTENTE, sahel, -2, 0);
            projet("Tableau de bord BI", 25000, 3, "Indicateurs de performance logistique en temps réel.", TERMINE, sahel, 6, 100, sarra);
            projet("Site vitrine & réservation", 18000, 3, "Site multilingue avec moteur de réservation d'excursions.", EN_COURS, djerba, 4, 70, sarra); // en retard
            Projet telemedecine = projet("Plateforme de télémédecine", 120000, 12, "Téléconsultation, dossier patient et prise de rendez-vous.", EN_COURS, atlas, 3, 25, amine, sarra);
            projet("Refonte du site web", 12000, 2, "Nouveau design responsive et optimisation SEO.", TERMINE, olivia, 5, 100, amine);
            projet("Application de traçabilité", 38000, 6, "Suivi des lots d'huile d'olive de la récolte à l'export.", EN_ATTENTE, olivia, -1, 0);

            // Tâches du Kanban : titre, statut, priorité, échéance (dans N jours, négatif = dépassée), responsable
            tache(mobile, "Maquettes Figma des écrans", Tache.Statut.TERMINEE, Tache.Priorite.MOYENNE, -60, sarra);
            tache(mobile, "Authentification biométrique", Tache.Statut.TERMINEE, Tache.Priorite.HAUTE, -20, amine);
            tache(mobile, "Consultation des comptes", Tache.Statut.TERMINEE, Tache.Priorite.HAUTE, -10, sarra);
            tache(mobile, "Virements entre comptes", Tache.Statut.EN_COURS, Tache.Priorite.HAUTE, 10, amine);
            tache(mobile, "Notifications push", Tache.Statut.EN_COURS, Tache.Priorite.MOYENNE, 25, sarra);
            tache(mobile, "Tests de sécurité (pentest)", Tache.Statut.A_FAIRE, Tache.Priorite.HAUTE, 45, null);
            tache(mobile, "Publication App Store / Play Store", Tache.Statut.A_FAIRE, Tache.Priorite.MOYENNE, 90, null);

            tache(boutique, "Catalogue produits", Tache.Statut.TERMINEE, Tache.Priorite.HAUTE, -70, sarra);
            tache(boutique, "Panier et commande", Tache.Statut.TERMINEE, Tache.Priorite.HAUTE, -45, sarra);
            tache(boutique, "Paiement en ligne", Tache.Statut.EN_COURS, Tache.Priorite.HAUTE, -12, sarra);
            tache(boutique, "Gestion des stocks", Tache.Statut.A_FAIRE, Tache.Priorite.MOYENNE, -5, null);
            tache(boutique, "Référencement SEO", Tache.Statut.A_FAIRE, Tache.Priorite.BASSE, 15, null);

            tache(telemedecine, "Dossier patient", Tache.Statut.TERMINEE, Tache.Priorite.HAUTE, -15, amine);
            tache(telemedecine, "Prise de rendez-vous", Tache.Statut.EN_COURS, Tache.Priorite.HAUTE, 20, sarra);
            tache(telemedecine, "Téléconsultation vidéo", Tache.Statut.A_FAIRE, Tache.Priorite.HAUTE, 80, null);
            tache(telemedecine, "Ordonnances électroniques", Tache.Statut.A_FAIRE, Tache.Priorite.MOYENNE, 120, null);
            for (Projet p : List.of(mobile, boutique, telemedecine)) p.recalculerAvancement();
            log.info("Données de démonstration créées (6 clients, 10 projets, tâches Kanban)");
        }
    }

    /**
     * Crée l'administrateur initial (psm.init-admin.*) s'il n'existe pas encore.
     * Un compte déjà présent n'est jamais modifié : son mot de passe reste celui
     * choisi ensuite depuis la page "Mon profil".
     */
    private void creerAdminInitial(Role admin) {
        String email = initAdminEmail == null ? "" : initAdminEmail.toLowerCase().trim();
        if (email.isEmpty() || initAdminPassword == null || initAdminPassword.isBlank()) {
            if (userRepository.count() == 0) {
                log.warn("Aucun utilisateur et aucun admin initial configuré : renseignez "
                        + "psm.init-admin.email et psm.init-admin.password (ou INIT_ADMIN_EMAIL / INIT_ADMIN_PASSWORD).");
            }
            return;
        }
        if (userRepository.existsByEmail(email)) {
            log.info("Admin initial déjà présent : {}", email);
            return;
        }
        if (initAdminPassword.length() < 8) {
            log.warn("Admin initial NON créé : le mot de passe doit contenir au moins 8 caractères.");
            return;
        }
        User u = new User(email, encoder.encode(initAdminPassword), null,
                initAdminNom.trim(), initAdminPrenom.trim(), true);
        u.getRoles().add(admin);
        userRepository.save(u);
        log.info("Admin initial créé : {}", email);
    }

    private Client client(String nom, String tel, String email, String adresse, String secteur) {
        return clientRepository.save(new Client(nom, tel, email, adresse, secteur));
    }

    /**
     * @param debutIlYaMois date de début = aujourd'hui moins N mois (négatif = démarrage futur) ;
     *                      la fin prévue = début + durée
     */
    private Projet projet(String libelle, double budget, int duree, String desc, StatutProjet statut, Client c,
                          int debutIlYaMois, int avancement, User... equipe) {
        Projet p = new Projet(libelle, budget, duree, desc, statut, c);
        LocalDate debut = LocalDate.now().minusMonths(debutIlYaMois).withDayOfMonth(1);
        p.setDateDebut(debut);
        p.setDateFinPrevue(debut.plusMonths(duree));
        p.setAvancement(avancement);
        for (User u : equipe) {
            if (u != null) p.getMembres().add(u);
        }
        Projet saved = projetRepository.save(p);
        c.getProjets().add(saved);
        return saved;
    }

    /** Ajoute une tâche en bas de sa colonne du Kanban */
    private void tache(Projet p, String titre, Tache.Statut statut, Tache.Priorite priorite, int echeanceDansJours,
                       User assigne) {
        Tache t = new Tache();
        t.setProjet(p);
        t.setTitre(titre);
        t.setStatut(statut);
        t.setPriorite(priorite);
        t.setEcheance(LocalDate.now().plusDays(echeanceDansJours));
        t.setAssigne(assigne);
        t.setOrdre((int) p.getTaches().stream().filter(x -> x.getStatut() == statut).count());
        p.getTaches().add(t);
        tacheRepository.save(t);
    }
}
