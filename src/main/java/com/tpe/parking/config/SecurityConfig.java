package com.tpe.parking.config;

import com.tpe.parking.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // === Ressources statiques et pages publiques ===
                .requestMatchers("/", "/accueil", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers("/403").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // === API REST — règles par endpoint ===

                // Utilisateurs API : ADMIN, MANAGER
                .requestMatchers("/api/utilisateurs/**").hasAnyRole("ADMIN", "MANAGER")

                // Véhicules API : ADMIN, MANAGER, OPERATOR
                .requestMatchers("/api/vehicules/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")

                // Places API — lecture (GET) : ADMIN, MANAGER, OPERATOR, USER (places disponibles)
                .requestMatchers(HttpMethod.GET, "/api/parking/spaces/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR", "USER")
                // Places API — écriture (POST, PUT, PATCH, DELETE) : ADMIN, MANAGER, OPERATOR
                .requestMatchers(HttpMethod.POST, "/api/parking/spaces/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers(HttpMethod.PUT, "/api/parking/spaces/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers(HttpMethod.PATCH, "/api/parking/spaces/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/parking/spaces/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")

                // Réservations API : ADMIN, MANAGER, USER
                .requestMatchers("/api/reservations/**").hasAnyRole("ADMIN", "MANAGER", "USER")

                // Abonnements API — lecture : ADMIN, MANAGER, USER
                .requestMatchers(HttpMethod.GET, "/api/subscriptions/**").hasAnyRole("ADMIN", "MANAGER", "USER")
                // Abonnements API — écriture : ADMIN, MANAGER
                .requestMatchers(HttpMethod.POST, "/api/subscriptions/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/subscriptions/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/api/subscriptions/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/subscriptions/**").hasAnyRole("ADMIN", "MANAGER")

                // Entrées/Sorties API : ADMIN, MANAGER, OPERATOR
                .requestMatchers("/api/parking/entries/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/api/parking/exits/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/api/parking/occupancy").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/api/parking/revenue").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")
                .requestMatchers("/api/parking/billing/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")

                // === Thymeleaf MVC — règles spécifiques AVANT génériques ===

                // Utilisateurs MVC : ADMIN, MANAGER
                .requestMatchers("/utilisateurs", "/utilisateurs/**").hasAnyRole("ADMIN", "MANAGER")

                // Véhicules MVC : ADMIN, MANAGER, OPERATOR
                .requestMatchers("/vehicules", "/vehicules/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")

                // Places MVC — consultation USER (SPÉCIFIQUE en premier)
                .requestMatchers("/places/disponibles-consultation").hasAnyRole("ADMIN", "MANAGER", "OPERATOR", "USER")
                // Places MVC — gestion (GÉNÉRIQUE après)
                .requestMatchers("/places", "/places/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")

                // Réservations MVC : ADMIN, MANAGER, USER
                .requestMatchers("/reservations", "/reservations/**").hasAnyRole("ADMIN", "MANAGER", "USER")

                // Abonnements MVC — mon-abonnement USER (SPÉCIFIQUE en premier)
                .requestMatchers("/abonnements/mon-abonnement").hasAnyRole("ADMIN", "MANAGER", "USER")
                // Abonnements MVC — gestion (GÉNÉRIQUE après)
                .requestMatchers("/abonnements", "/abonnements/**").hasAnyRole("ADMIN", "MANAGER")

                // Entrées/Sorties MVC : ADMIN, MANAGER, OPERATOR
                .requestMatchers("/entrees-sorties", "/entrees-sorties/**").hasAnyRole("ADMIN", "MANAGER", "OPERATOR")

                // Garde-fou : toute autre demande doit être authentifiée
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/accueil", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/403")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
