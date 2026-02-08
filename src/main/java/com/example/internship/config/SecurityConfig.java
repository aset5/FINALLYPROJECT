package com.example.internship.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Настройка прав доступа
                .authorizeHttpRequests(auth -> auth
                        // Разрешаем всем: главную, регистрацию, логин и статику
                        .requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                        // Только админам
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Только компаниям
                        .requestMatchers("/company/**").hasRole("COMPANY")
                        // Все остальное (включая детали вакансий /internship/**) требует авторизации
                        .anyRequest().authenticated()
                )
                // 2. Настройка формы логина
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true) // Принудительный возврат на главную
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                // 3. Настройка выхода
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                // 4. Важно для работы H2 консоли или простых форм (если возникнут проблемы с POST)
                .csrf(csrf -> csrf.disable()); // Для диплома на локалке лучше отключить, чтобы формы работали проще

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}