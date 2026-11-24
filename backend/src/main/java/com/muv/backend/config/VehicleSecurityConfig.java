package com.muv.backend.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class VehicleSecurityConfig {
    @Bean
    @Order(2)
    SecurityFilterChain vehicleRoutes(
            HttpSecurity http,
            @Value("${muv.frontend-origin:http://localhost:5173}") String frontendOrigin,
            @Value("${CODESPACE_NAME:}") String codespaceName,
            @Value("${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN:app.github.dev}") String forwardingDomain)
            throws Exception {
        var cors = new CorsConfiguration();
        cors.addAllowedOrigin(frontendOrigin);
        if (!codespaceName.isBlank()) {
            cors.addAllowedOrigin("https://" + codespaceName + "-5173." + forwardingDomain);
            cors.addAllowedOrigin("https://localhost:5173");
        }
        cors.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT"));
        cors.setAllowedHeaders(List.of("Accept", "Content-Type", "X-CSRF-TOKEN"));
        cors.setAllowCredentials(true);
        var corsSource = new UrlBasedCorsConfigurationSource();
        corsSource.registerCorsConfiguration("/api/veiculos/**", cors);

        return http.securityMatcher("/api/veiculos/**")
                .cors(config -> config.configurationSource(corsSource))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> response.setStatus(401))
                        .accessDeniedHandler((request, response, exception) -> response.setStatus(403)))
                .build();
    }
}
