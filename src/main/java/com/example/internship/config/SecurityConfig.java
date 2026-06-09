package com.example.internship.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DisabledAccountFilter disabledAccountFilter;

    public SecurityConfig(DisabledAccountFilter disabledAccountFilter) {
        this.disabledAccountFilter = disabledAccountFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .addFilterAfter(disabledAccountFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/send-verification-code",
                                "/api/auth/verify-email-code",
                                "/api/auth/universities",
                                "/api/auth/login",
                                "/api/home",
                                "/api/certificates/**",
                                "/uploads/**",
                                "/assets/**",
                                "/index.html",
                                "/vite.svg",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/api/university-admin/**").hasRole("UNIVERSITY_ADMIN")
                        .requestMatchers("/api/company/**").hasRole("COMPANY")
                        .requestMatchers("/api/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/ai/**").authenticated()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                JsonResponses.write(
                                        objectMapper,
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        Map.of("message", "Требуется авторизация"));
                            } else {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        })
                )
                .formLogin(login -> login
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK))
                        .failureHandler((request, response, exception) -> {
                            String message = "Неверный логин или пароль";
                            if (exception instanceof org.springframework.security.authentication.DisabledException) {
                                message = "Аккаунт не активирован. Для компаний и ВУЗов нужно дождаться одобрения администратора.";
                            }
                            JsonResponses.write(
                                    objectMapper,
                                    response,
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    Map.of("message", message));
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK))
                        .permitAll()
                );

        return http.build();
    }
}
