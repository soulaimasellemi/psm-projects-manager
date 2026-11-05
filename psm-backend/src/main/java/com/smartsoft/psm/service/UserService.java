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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ---------- Administration ----------

    @Transactional(readOnly = true)
    public PageResponse<UserDto> findAll(String q, Boolean enabled, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("enabled").ascending().and(Sort.by("id").descending()));
        return PageResponse.of(userRepository.search(q == null ? "" : q.trim(), enabled, pageable).map(UserDto::from));
    }

    public UserDto changerStatut(Long id, boolean enabled, Long currentUserId) {
        if (id.equals(currentUserId) && !enabled) {
            throw ApiException.badRequest("Vous ne pouvez pas désactiver votre propre compte.");
        }
        User u = getEntity(id);
        boolean activation = !u.isEnabled() && enabled;
        u.setEnabled(enabled);
        if (activation) emailService.notifierActivation(u);
        return UserDto.from(u);
    }

    public UserDto changerRoles(Long id, Set<String> libelles, Long currentUserId) {
        if (id.equals(currentUserId) && !libelles.contains(Role.ADMIN)) {
            throw ApiException.badRequest("Vous ne pouvez pas retirer votre propre rôle ADMIN.");
        }
        Set<Role> roles = new HashSet<>();
        for (String l : libelles) {
            roles.add(roleRepository.findByLibelle(l.toUpperCase())
                    .orElseThrow(() -> ApiException.badRequest("Rôle inconnu : " + l)));
        }
        User u = getEntity(id);
        u.setRoles(roles);
        return UserDto.from(u);
    }

    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw ApiException.badRequest("Vous ne pouvez pas supprimer votre propre compte.");
        }
        userRepository.delete(getEntity(id));
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
    }

    private User getEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable (id=" + id + ")"));
    }
}
