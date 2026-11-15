package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.AuthDtos.AuthResponse;
import com.smartsoft.psm.dto.AuthDtos.LoginRequest;
import com.smartsoft.psm.dto.AuthDtos.RegisterRequest;
import com.smartsoft.psm.dto.UserDto;
import com.smartsoft.psm.entity.Activite.Action;
import com.smartsoft.psm.entity.Activite.Cible;
import com.smartsoft.psm.entity.Role;
import com.smartsoft.psm.entity.User;
import com.smartsoft.psm.exception.ApiException;
import com.smartsoft.psm.repository.RoleRepository;
import com.smartsoft.psm.repository.UserRepository;
import com.smartsoft.psm.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final ActiviteService activiteService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RESET_VALIDITE_MINUTES = 30;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, EmailService emailService, ActiviteService activiteService) {
        this.activiteService = activiteService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    public AuthResponse login(LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email().toLowerCase().trim(), req.password()));
            User user = (User) auth.getPrincipal();
            activiteService.enregistrerPour(user, Action.CONNEXION, Cible.UTILISATEUR, user.getId(), "Connexion réussie");
            return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
        } catch (DisabledException e) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Votre compte est en attente de validation par l'administrateur.");
        } catch (BadCredentialsException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        }
    }

    /**
     * Inscription : le compte est créé INACTIF avec le rôle COLLABORATEUR,
     * puis un email est envoyé à l'administrateur pour validation.
     */
    @Transactional
    public UserDto register(RegisterRequest req) {
        String email = req.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email.");
        }
        Role collab = roleRepository.findByLibelle(Role.COLLABORATEUR)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Rôle COLLABORATEUR manquant"));

        User user = new User(email, passwordEncoder.encode(req.password()), req.tel(),
                req.nom().trim(), req.prenom().trim(), false);
        user.getRoles().add(collab);
        userRepository.save(user);

        emailService.notifierAdminInscription(user);
        activiteService.enregistrerPour(user, Action.INSCRIPTION, Cible.UTILISATEUR, user.getId(),
                "Nouvelle inscription, en attente de validation");
        return UserDto.from(user);
    }

    // ---------- Mot de passe oublié ----------

    /**
     * Étape 1 : génère un jeton aléatoire, en stocke l'empreinte SHA-256 (valable 30 min)
     * et envoie le lien par email. La réponse est toujours la même, que l'email existe ou non,
     * pour ne pas révéler quels comptes existent.
     */
    @Transactional
    public void demanderReinitialisation(String emailSaisi) {
        String email = emailSaisi.toLowerCase().trim();
        userRepository.findByEmail(email).filter(User::isEnabled).ifPresent(user -> {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            user.setResetTokenHash(sha256(token));
            user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(RESET_VALIDITE_MINUTES));
            emailService.envoyerLienReinitialisation(user, token, RESET_VALIDITE_MINUTES);
            activiteService.enregistrerPour(user, Action.MOT_DE_PASSE, Cible.UTILISATEUR, user.getId(),
                    "Demande de réinitialisation du mot de passe (lien envoyé par email)");
        });
    }

    /** Étape 2 : vérifie le jeton (existe + non expiré), change le mot de passe et invalide le jeton. */
    @Transactional
    public void reinitialiser(String token, String nouveauMotDePasse) {
        User user = userRepository.findByResetTokenHash(sha256(token.trim()))
                .filter(u -> u.getResetTokenExpiration() != null && u.getResetTokenExpiration().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> ApiException.badRequest(
                        "Ce lien de réinitialisation est invalide ou a expiré. Faites une nouvelle demande."));
        user.setPassword(passwordEncoder.encode(nouveauMotDePasse));
        user.setResetTokenHash(null);
        user.setResetTokenExpiration(null);
        activiteService.enregistrerPour(user, Action.MOT_DE_PASSE, Cible.UTILISATEUR, user.getId(),
                "Mot de passe réinitialisé via le lien reçu par email");
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
