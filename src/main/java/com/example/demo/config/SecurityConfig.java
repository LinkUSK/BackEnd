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
                // 🔥 반드시 있어야 CORS 설정이 CorsConfig에서 읽힘
                .cors(Customizer.withDefaults())

                // CSRF는 REST API에서 비활성화
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // WebSocket 허용
                        .requestMatchers("/ws/**").permitAll()

                        // 인증(Login/Signup 등)은 모두 허용
                        .requestMatchers("/api/auth/**").permitAll()

                        // AI 추천 태그 등도 허용
                        .requestMatchers("/api/ai/**").permitAll()

                        // CORS Preflight(OPTIONS) 요청을 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // meta 정보는 모두 허용
                        .requestMatchers(HttpMethod.GET, "/api/meta/**").permitAll()

                        // 회원가입 중 public upload 허용
                        .requestMatchers(HttpMethod.POST, "/api/files/upload-public").permitAll()

                        // 업로드 파일 조회
                        .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/files/**").permitAll()

                        // 나머지 모든 API → JWT 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터 적용
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
