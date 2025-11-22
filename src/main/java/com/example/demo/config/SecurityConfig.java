// src/main/java/com/example/demo/config/SecurityConfig.java
package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CORS
                .cors(Customizer.withDefaults())

                // CSRF (REST API + JWT는 비활성화)
                .csrf(csrf -> csrf.disable())

                // JWT만 사용 → 세션 사용 안 함
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // 🔹 WebSocket 엔드포인트 허용
                        .requestMatchers("/ws/**").permitAll()

                        // 🔹 인증 관련 공개 API
                        .requestMatchers("/api/auth/**").permitAll()

                        // 🔹 AI 공개 API
                        .requestMatchers("/api/ai/**").permitAll()

                        // 🔹 OPTIONS (CORS Preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔹 meta 정보 GET 허용
                        .requestMatchers(HttpMethod.GET, "/api/meta/**").permitAll()

                        // 🔹 회원가입 중 public upload 허용
                        .requestMatchers(HttpMethod.POST, "/api/files/upload-public").permitAll()

                        // 🔹 이미지/업로드 파일 조회 허용 (⭐ 중요)
                        .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/files/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()  // ⭐ 추가됨
                        .requestMatchers(HttpMethod.GET, "/images/**").permitAll()   // ⭐ 혹시 필요하면

                        // 나머지는 JWT 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
