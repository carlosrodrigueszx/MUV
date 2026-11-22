package com.muv.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AchadoPerdidoSecurityConfig {
    @Bean
    @Order(2)
    SecurityFilterChain achadoPerdidoRoutes(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/achados-perdidos", "/api/achados-perdidos/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/achados-perdidos", "/api/achados-perdidos/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/achados-perdidos").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/achados-perdidos/*/devolucao").authenticated()
                        .anyRequest().denyAll())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> response.setStatus(401))
                        .accessDeniedHandler((request, response, exception) -> response.setStatus(403)))
                .build();
    }
}
