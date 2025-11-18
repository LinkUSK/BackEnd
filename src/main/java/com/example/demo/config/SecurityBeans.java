package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security의 PasswordEncoder 빈 등록
 * UserService 등에서 비밀번호 암호화에 사용됨
 */
@Configuration
public class SecurityBeans {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt는 강력한 단방향 해시 함수로, 로그인 비밀번호 비교에 사용됨
        return new BCryptPasswordEncoder();
    }
}
