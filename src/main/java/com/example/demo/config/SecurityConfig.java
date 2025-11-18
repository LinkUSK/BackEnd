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
                // CORS 기본값
                .cors(Customizer.withDefaults())

                // CSRF: WebSocket + API + 파일 경로는 예외
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**", "/api/**", "/files/**"))

                .authorizeHttpRequests(auth -> auth
                        // 웹소켓은 모두 허용
                        .requestMatchers("/ws/**").permitAll()

                        // 로그인 / 회원가입 / 이메일 인증 등의 auth API
                        .requestMatchers("/api/auth/**").permitAll()

                        // 🔹 AI 관련 API는 모두 허용 (태그 추천 등)
                        .requestMatchers("/api/ai/**").permitAll()

                        // CORS preflight(OPTIONS)는 전역 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔹 메타 정보(카테고리/태그) 조회는 모두 허용
                        //    - GET /api/meta/categories
                        //    - GET /api/meta/tags?category=...
                        .requestMatchers(HttpMethod.GET, "/api/meta/**").permitAll()

                        // 🔹 회원가입 단계에서 사용하는 "비로그인" 파일 업로드 허용
                        .requestMatchers(HttpMethod.POST, "/api/files/upload-public").permitAll()

                        // 🔹 업로드된 파일 조회(GET)는 모두 허용
                        .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/files/**").permitAll()

                        // 그 외 나머지 요청은 JWT 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 전에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
