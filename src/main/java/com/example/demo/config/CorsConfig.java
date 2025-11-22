package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // ✅ 허용할 Origin들
        cfg.setAllowedOrigins(List.of(
                "http://127.0.0.1:5500",
                "http://localhost:5500",
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://link-u.netlify.app"   // 🔥 배포된 프론트
        ));

        // ✅ 허용 메서드
        cfg.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ✅ 허용 헤더 (여기가 핵심: 전부 허용)
        cfg.setAllowedHeaders(List.of("*"));

        // 프론트에서 Authorization 헤더를 읽을 수 있게 노출
        cfg.setExposedHeaders(List.of("Authorization"));

        // JWT + 쿠키 쓸 때 필요
        cfg.setAllowCredentials(true);

        // 프리플라이트 결과 캐시 시간 (선택)
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
