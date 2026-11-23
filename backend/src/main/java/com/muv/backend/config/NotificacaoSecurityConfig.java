package com.muv.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class NotificacaoSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain notificacaoRoutes(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/notificacoes/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/notificacoes/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/notificacoes/**").authenticated()
                        .anyRequest().denyAll())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) -> res.setStatus(401))
                        .accessDeniedHandler((req, res, ex) -> res.setStatus(403)))
                .build();
    }
}
