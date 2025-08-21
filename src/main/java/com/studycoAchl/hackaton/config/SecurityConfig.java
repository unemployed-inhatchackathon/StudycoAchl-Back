package com.studycoAchl.hackaton.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 개발용 설정: CSRF 완전 비활성화
                .csrf(csrf -> csrf.disable())

                // CORS는 별도 CorsConfig에서 처리
                .cors(cors -> {})

                // URL별 접근 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // Health check는 인증 없이 접근 가능
                        .requestMatchers("/api/health").permitAll()

                        // Swagger UI 접근 허용
                        .requestMatchers("/api/swagger-ui/**", "/api/api-docs/**", "/api/swagger-ui.html").permitAll()

                        // API 문서나 개발 도구 허용
                        .requestMatchers("/api/api/problem-session/health").permitAll()

                        // 나머지 API는 Basic 인증 필요
                        .requestMatchers("/api/api/problem-session/**").authenticated()

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // HTTP Basic 인증 활성화 (개발 편의성)
                .httpBasic(httpBasic -> {})

                // 헤더 설정
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                        .contentTypeOptions(contentType -> {})
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // 인메모리 사용자 생성
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin")) // 비밀번호 암호화
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}