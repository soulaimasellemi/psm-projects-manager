package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.PageResponse;
import com.smartsoft.psm.dto.UserDto;
import com.smartsoft.psm.dto.UserDto.PasswordRequest;
import com.smartsoft.psm.dto.UserDto.ProfilRequest;
import com.smartsoft.psm.entity.Role;
import com.smartsoft.psm.entity.User;
import com.smartsoft.psm.exception.ApiException;
import com.smartsoft.psm.repository.RoleRepository;
import com.smartsoft.psm.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.smartsoft.psm.dto.ProjetDto;
import com.smartsoft.psm.entity.Activite.Action;
import com.smartsoft.psm.entity.Activite.Cible;
import com.smartsoft.psm.repository.ProjetRepository;
import com.smartsoft.psm.repository.TacheRepository;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ActiviteService activiteService;
    private final ProjetRepository projetRepository;
    private final TacheRepository tacheRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, EmailService emailService,
                       ActiviteService activiteService, ProjetRepository projetRepository,
                       TacheRepository tacheRepository) {
        this.tacheRepository = tacheRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.activiteService = activiteService;
        this.projetRepository = projetRepository;
    }

    // ---------- Collaborateurs (tout utilisateur connecté) ----------

    /** Comptes actifs pouvant être affectés à un projet */
    @Transactional(readOnly = true)
    public List<ProjetDto.Membre> options() {
        return userRepository.findByEnabledTrueOrderByPrenomAscNomAsc().stream().map(ProjetDto.Membre::from).toList();
    }

    // ---------- Administration ----------

    @Transactional(readOnly = true)
    public PageResponse<UserDto> findAll(String q, Boolean enabled, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("enabled").ascending().and(Sort.by("id").descending()));
        return PageResponse.of(userRepository.search(q == null ? "" : q.trim(), enabled, pageable).map(UserDto::from));
    }

    /** Création d'un compte par l'ADMIN : actif immédiatement, mot de passe haché (BCrypt). */
    public UserDto create(UserDto.CreateUserRequest req) {
        String email = req.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email.");
        }
        User u = new User(email, passwordEncoder.encode(req.password()), ClientService.blankToNull(req.tel()),
                req.nom().trim(), req.prenom().trim(), true);
        u.setRoles(resoudreRoles(req.roles()));
        User saved = userRepository.save(u);
        activiteService.enregistrer(Action.CREATION, Cible.UTILISATEUR, saved.getId(),
                "Compte " + saved.getNomComplet() + " (" + saved.getEmail() + "), rôles : " + libellesRoles(saved));
        return UserDto.from(saved);
    }

    public UserDto changerStatut(Long id, boolean enabled, Long currentUserId) {
        if (id.equals(currentUserId) && !enabled) {
            throw ApiException.badRequest("Vous ne pouvez pas désactiver votre propre compte.");
        }
        User u = getEntity(id);
        boolean activation = !u.isEnabled() && enabled;
        u.setEnabled(enabled);
        if (enabled) u.effacerJetonValidation(); // le lien de l'email admin ne sert plus
        if (activation) emailService.notifierActivation(u);
        activiteService.enregistrer(enabled ? Action.VALIDATION : Action.MODIFICATION, Cible.UTILISATEUR, id,
                (enabled ? "Compte activé : " : "Compte désactivé : ") + u.getNomComplet() + " (" + u.getEmail() + ")");
        return UserDto.from(u);
    }

    public UserDto changerRoles(Long id, Set<String> libelles, Long currentUserId) {
        if (id.equals(currentUserId) && !libelles.contains(Role.ADMIN)) {
            throw ApiException.badRequest("Vous ne pouvez pas retirer votre propre rôle ADMIN.");
        }
        User u = getEntity(id);
        u.setRoles(resoudreRoles(libelles));
        activiteService.enregistrer(Action.MODIFICATION, Cible.UTILISATEUR, id,
                "Rôles de " + u.getNomComplet() + " : " + libellesRoles(u));
        return UserDto.from(u);
    }

    private static String libellesRoles(User u) {
        return String.join(", ", u.getRoles().stream().map(Role::getLibelle).sorted().toList());
    }

    private Set<Role> resoudreRoles(Set<String> libelles) {
        Set<Role> roles = new HashSet<>();
        for (String l : libelles) {
            roles.add(roleRepository.findByLibelle(l.toUpperCase().trim())
                    .orElseThrow(() -> ApiException.badRequest("Rôle inconnu : " + l)));
        }
        return roles;
    }

    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw ApiException.badRequest("Vous ne pouvez pas supprimer votre propre compte.");
        }
        User u = getEntity(id);
        // Retire d'abord le compte des projets auxquels il est affecté (table projet_membres)
        projetRepository.findByMembresContaining(u).forEach(p -> p.getMembres().remove(u));
        tacheRepository.findByAssigne(u).forEach(t -> t.setAssigne(null));
        userRepository.delete(u);
        activiteService.enregistrer(Action.SUPPRESSION, Cible.UTILISATEUR, id,
                "Compte " + u.getNomComplet() + " (" + u.getEmail() + ")");
    }

    // ---------- Profil de l'utilisateur connecté ----------

    @Transactional(readOnly = true)
    public UserDto me(Long id) {
        return UserDto.from(getEntity(id));
    }

    public UserDto updateProfil(Long id, ProfilRequest req) {
        User u = getEntity(id);
        u.setNom(req.nom().trim());
        u.setPrenom(req.prenom().trim());
        u.setTel(ClientService.blankToNull(req.tel()));
        return UserDto.from(u);
    }

    public void changePassword(Long id, PasswordRequest req) {
        User u = getEntity(id);
        if (!passwordEncoder.matches(req.ancien(), u.getPassword())) {
            throw ApiException.badRequest("L'ancien mot de passe est incorrect.");
        }
        u.setPassword(passwordEncoder.encode(req.nouveau()));
        activiteService.enregistrer(Action.MOT_DE_PASSE, Cible.UTILISATEUR, id, "Mot de passe modifié depuis le profil");
    }

    private User getEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable (id=" + id + ")"));
    }
}
