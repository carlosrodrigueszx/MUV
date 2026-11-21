package com.muv.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class FreightCancellationSecurityConfig {
    @Bean
    SecurityFilterChain freightCancellationRoutes(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/fretes/*/cancelamento")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/fretes/*/cancelamento").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/fretes/*/cancelamento").authenticated()
                        .anyRequest().denyAll())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((req, res, ex) -> res.setStatus(401)))
                .build();
    }
}
