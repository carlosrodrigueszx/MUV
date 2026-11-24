package com.muv.backend.config;

import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.AuthService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableAsync
public class AuthSecurityConfig {
    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService users(AuthRepository accounts) {
        return email -> accounts.findAccount(AuthService.normalizeEmail(email))
                .map(account -> User.withUsername(AuthService.normalizeEmail(account.email()))
                        .password(account.senha()).roles("USUARIO").build())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }

    @Bean
    SecurityFilterChain authRoutes(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/auth/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/cadastro", "/api/auth/login",
                                "/api/auth/cadastro/empresa", "/api/auth/cadastro/autonomo", "/api/auth/recuperacao", "/api/auth/redefinicao").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/sessao", "/api/auth/visao-geral").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/perfil/autonomo").authenticated()
                        .anyRequest().denyAll())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((req, res, ex) -> res.setStatus(401)))
                .formLogin(login -> login.loginProcessingUrl("/api/auth/login").usernameParameter("email")
                        .passwordParameter("senha").successHandler((req, res, auth) -> res.setStatus(204))
                        .failureHandler((req, res, ex) -> res.setStatus(401)))
                .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
                .build();
    }
}
