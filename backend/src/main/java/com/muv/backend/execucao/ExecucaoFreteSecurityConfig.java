package com.muv.backend.execucao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ExecucaoFreteSecurityConfig {
    @Bean
    SecurityFilterChain execucaoFreteRoutes(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/fretes/*/execucao", "/api/fretes/*/execucao/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/fretes/*/execucao").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/fretes/*/execucao/inicio",
                                "/api/fretes/*/execucao/interrupcoes", "/api/fretes/*/execucao/ocorrencias",
                                "/api/fretes/*/execucao/retomadas",
                                "/api/fretes/*/execucao/encerramento-sem-conclusao").authenticated()
                        .anyRequest().denyAll())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((req, res, ex) -> res.setStatus(401)))
                .build();
    }
}
