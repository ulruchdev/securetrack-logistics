package com.cbs.logistics.tracking_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sécurité Basic Auth pour le Tracking Service.
 *
 * <p>Protège tous les endpoints sauf /actuator/health et /swagger-ui.
 * Identique au pattern utilisé par security-checkpoint-service.</p>
 *
 * <p>Configuration via variables d'environnement :</p>
 * <ul>
 *   <li>SECURITY_USERNAME (défaut: admin)</li>
 *   <li>SECURITY_PASSWORD (obligatoire)</li>
 * </ul>
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Value("${security.tracking.username:admin}")
    private String username;

    @Value("${security.tracking.password}")
    private String password;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> {});

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails admin = User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles("TRACKING_OPERATOR")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}
