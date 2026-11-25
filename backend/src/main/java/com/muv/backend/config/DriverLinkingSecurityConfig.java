package com.muv.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class DriverLinkingSecurityConfig {
    @Bean
    @Order(2)
    SecurityFilterChain driverLinkingRoutes(HttpSecurity http) throws Exception {
        return http.securityMatcher(
                        "/api/prestadores/motoristas/perfil",
                        "/api/prestadores/*/motoristas",
                        "/api/prestadores/*/motoristas/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(
                        (request, response, exception) -> response.setStatus(401)))
                .build();
    }
}
