package com.muv.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class PropostaSecurityConfig {
    @Bean
    @Order(2)
    SecurityFilterChain propostaRoutes(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/prestador/solicitacoes", "/api/prestador/solicitacoes/perfil", "/api/prestador/solicitacoes/*/avaliacao", "/api/prestador/solicitacoes/*/avaliacao/percurso", "/api/prestador/solicitacoes/*/propostas", "/api/prestador/solicitacoes/*/recusa")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((req, res, ex) -> res.setStatus(401)))
                .build();
    }
}
