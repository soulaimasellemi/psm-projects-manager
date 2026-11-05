package com.smartsoft.psm.service;

import com.smartsoft.psm.dto.AuthDtos.AuthResponse;
import com.smartsoft.psm.dto.AuthDtos.LoginRequest;
import com.smartsoft.psm.dto.AuthDtos.RegisterRequest;
import com.smartsoft.psm.dto.UserDto;
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

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, EmailService emailService) {
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
        return UserDto.from(user);
    }
}
