// src/main/java/com/example/demo/security/JwtAuthenticationFilter.java
package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 매 요청마다 Authorization: Bearer 토큰을 검사하여
 * SecurityContext에 인증 정보(Principal=userId)를 설정.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    // JWT subject (userId / email 등)
                    String username = jwtTokenProvider.getUsername(token);

                    // SecurityContext에 Authentication 저장
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // 토큰이 잘못됐더라도 로그인 없이 계속 진행 (401은 EntryPoint에서 처리)
                SecurityContextHolder.clearContext();
            }
        }

        // 다음 필터로 전달
        filterChain.doFilter(request, response);
    }
}
