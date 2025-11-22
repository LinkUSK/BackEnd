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
                // CORS 설정 (CorsConfig와 연동)
                .cors(Customizer.withDefaults())

                // CSRF 비활성화 (REST API + JWT라서)
                .csrf(csrf -> csrf.disable())

                // 세션은 사용하지 않고, JWT로만 인증
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // 🔹 WebSocket 엔드포인트는 모두 허용
                        //    (STOMP 접속 시 Authorization 헤더는 StompAuthChannelInterceptor에서 처리)
                        .requestMatchers("/ws/**").permitAll()

                        // 인증/회원가입 관련 API는 공개
                        .requestMatchers("/api/auth/**").permitAll()

                        // AI 관련 공개 API
                        .requestMatchers("/api/ai/**").permitAll()

                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // meta 정보 GET은 공개
                        .requestMatchers(HttpMethod.GET, "/api/meta/**").permitAll()

                        // 회원가입 중 public upload 허용
                        .requestMatchers(HttpMethod.POST, "/api/files/upload-public").permitAll()

                        // 업로드 파일 조회는 공개
                        .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/files/**").permitAll()

                        // 나머지 모든 요청은 JWT 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
