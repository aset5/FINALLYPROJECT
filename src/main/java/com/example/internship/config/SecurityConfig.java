package com.example.internship.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Оставляем выключенным для простоты разработки
                .authorizeHttpRequests(auth -> auth
                        // Публичные страницы
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()

                        // Защищенные разделы
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/company/**").hasRole("COMPANY")
                        .requestMatchers("/student/**").hasRole("STUDENT") // ОБЯЗАТЕЛЬНО для доступа студента

                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            // Получаем список ролей текущего пользователя
                            var roles = authentication.getAuthorities().stream()
                                    .map(r -> r.getAuthority())
                                    .toList();

                            // Логика перенаправления после входа
                            if (roles.contains("ROLE_ADMIN")) {
                                response.sendRedirect("/admin/dashboard");
                            } else if (roles.contains("ROLE_COMPANY")) {
                                response.sendRedirect("/company/dashboard");
                            } else if (roles.contains("ROLE_STUDENT")) {
                                response.sendRedirect("/"); // Студенты идут на главную смотреть вакансии
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout") // Добавил параметр для уведомления
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/?error=no_access") // Куда отправлять если нет прав
                );

        return http.build();
    }
}