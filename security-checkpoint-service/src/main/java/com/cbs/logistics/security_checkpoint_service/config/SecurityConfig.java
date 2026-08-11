package com.cbs.logistics.security_checkpoint_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults());

        // Exiger HTTPS sur les endpoints protégés (actif en production uniquement)
        if (requireHttps) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Nom d'utilisateur du compte d'administration (surchargeable via l'environnement). */
    @Value("${security.checkpoint.username:admin}")
    private String username;

    /** Mot de passe OBLIGATOIRE (le service refuse de démarrer si SECURITY_PASSWORD n'est pas définie). */
    @Value("${security.checkpoint.password}")
    private String password;

    /** Exiger HTTPS sur les endpoints protégés (true en production, false en dev local). */
    @Value("${security.checkpoint.require-https:false}")
    private boolean requireHttps;

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails admin = User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles("CHECKPOINT_OPERATOR")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}