package com.muv.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class DesignacaoSecurityConfig {
    @Bean
    @Order(2)
    SecurityFilterChain designacaoRoutes(HttpSecurity http) throws Exception {
        return http.securityMatcher(
                        "/api/prestador/fretes/**",
                        "/api/cliente/fretes/*/designacao",
                        "/api/cliente/fretes/*/acordos/*/resposta")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> response.setStatus(401))
                        .accessDeniedHandler((request, response, exception) -> response.setStatus(403)))
                .build();
    }
}
