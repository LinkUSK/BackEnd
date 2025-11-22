package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
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

    /**
     * 🔥 특정 요청은 JWT 필터가 아예 실행되지 않도록 스킵한다.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {

        // ✅ 여기서부터가 핵심
        //   - getServletPath() 는 "" 가 나오는 경우가 많다.
        //   - 실제로는 getRequestURI() 에 "/ws/info" 같은 전체 경로가 들어있음.
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1) WebSocket(SockJS) 엔드포인트 → JWT 필터 적용 금지
        if (uri.startsWith("/ws")) {
            return true;
        }

        // 2) CORS Preflight → JWT 검사 X
        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }

        // 3) 인증/회원가입 API는 공개
        if (uri.startsWith("/api/auth/")) {
            return true;
        }

        return false;  // 위 조건 제외하고는 기존 필터 로직 실행
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                if (jwtTokenProvider.validateToken(token)) {

                    // JWT subject (userId / email 등)
                    String username = jwtTokenProvider.getUsername(token);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // 토큰이 잘못된 경우 인증 제거
                SecurityContextHolder.clearContext();
            }
        }

        // 다음 필터로 전달
        filterChain.doFilter(request, response);
    }
}
