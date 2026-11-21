package com.muv.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ContratacaoSecurityConfig {
    @Bean
    @Order(2)
    SecurityFilterChain contratacaoRoutes(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/cliente/solicitacoes/*/propostas", "/api/cliente/solicitacoes/*/propostas/*/aceite", "/api/cliente/fretes/*")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((req, res, ex) -> res.setStatus(401)))
                .build();
    }
}
