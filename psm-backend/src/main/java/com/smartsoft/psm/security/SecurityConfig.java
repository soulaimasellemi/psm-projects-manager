package com.smartsoft.psm.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Règles d'accès (cahier des charges) :
 *  - ADMIN         : CRUD complet sur clients et projets + gestion des utilisateurs
 *  - COLLABORATEUR : consulter (GET) et ajouter (POST) des clients et des projets
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String COLLAB = "COLLABORATEUR";
    private static final String[] RESSOURCES = {"/api/clients/**", "/api/projets/**"};

    @Value("${psm.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
                                           UserDetailsService userDetailsService) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public : login / register
                .requestMatchers("/api/auth/**").permitAll()
                // Profil de l'utilisateur connecté
                .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                // Gestion des utilisateurs : admin uniquement
                .requestMatchers("/api/users/**").hasRole(ADMIN)
                // Clients & projets
                .requestMatchers(HttpMethod.GET, RESSOURCES).hasAnyRole(ADMIN, COLLAB)
                .requestMatchers(HttpMethod.POST, RESSOURCES).hasAnyRole(ADMIN, COLLAB)
                .requestMatchers(HttpMethod.PUT, RESSOURCES).hasRole(ADMIN)
                .requestMatchers(HttpMethod.DELETE, RESSOURCES).hasRole(ADMIN)
                // Statistiques du dashboard
                .requestMatchers(HttpMethod.GET, "/api/stats/**").hasAnyRole(ADMIN, COLLAB)
                // Tout autre endpoint d'API nécessite d'être connecté
                .requestMatchers("/api/**").authenticated()
                // Le reste = application Angular (fichiers statiques) en production
                .anyRequest().permitAll())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        writeError(res, 401, "Authentification requise : token manquant, invalide ou expiré"))
                .accessDeniedHandler((req, res, e) ->
                        writeError(res, 403, "Accès refusé : vous n'avez pas les droits nécessaires")))
            .addFilterBefore(new JwtAuthFilter(jwtService, userDetailsService),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setCharacterEncoding("UTF-8");
        res.setContentType("application/json");
        res.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message.replace("\"", "'") + "\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition")); // nom du fichier téléchargé
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
