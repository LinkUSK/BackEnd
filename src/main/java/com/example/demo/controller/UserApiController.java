// src/main/java/com/example/demo/controller/UserApiController.java
package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.UserService;
import com.example.demo.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 🔹 회원 관련 REST API 컨트롤러
 *  - 이메일 인증코드 요청
 *  - 이메일 인증 후 회원가입
 *  - 로그인(JWT 발급)
 *  - userId 로 프로필 조회
 */
@RestController
@RequestMapping("/api/auth")
public class UserApiController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final VerificationService verificationService;

    public UserApiController(UserService userService,
                             JwtTokenProvider jwtTokenProvider,
                             VerificationService verificationService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.verificationService = verificationService;
    }

    /**
     * 📩 1단계: 학교 이메일로 인증코드 발송
     * - 클라이언트에서 이메일을 보내면
     * - VerificationService 가 코드 생성 + 저장 + 메일 발송까지 처리
     */
    @PostMapping("/request-code")
    public ResponseEntity<?> requestCode(@Valid @RequestBody RequestCodeRequest req) {
        verificationService.requestCode(req.email());
        return ResponseEntity.ok(Map.of("message", "인증코드를 발송했습니다."));
    }

    /**
     * ✅ 2단계: 이메일 + 코드 검증 후 바로 회원가입까지 처리
     * - 코드 검증이 성공하면 UserService 를 통해 실제 User 생성
     * - 생성된 유저 정보와 함께 JWT 토큰을 반환
     *   → 프론트는 이 토큰을 저장해 이후 인증이 필요한 API 호출에 사용
     */
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyAndSignup(@Valid @RequestBody VerifyCodeRequest req) {
        // 1) 이메일 + 코드 검증 (만료/중복 여부 포함)
        verificationService.verifyAndConsume(req.email(), req.code());

        // 2) 검증된 이메일 기준으로 회원 생성
        UserResponse created = userService.createUserAfterEmailVerified(
                req.email(), req.username(), req.password(), req.major(), req.profileImageUrl()
        );

        // 3) 로그인 상태를 유지할 수 있도록 토큰 발급
        String token = jwtTokenProvider.generateToken(created.userId());
        return ResponseEntity.ok(Map.of("token", token, "user", created));
    }

    /**
     * 🔐 3단계: 로그인
     * - 아이디/비밀번호 검증 후 성공하면 JWT 토큰 발급
     * - 프론트는 이 토큰을 이후 Authorization 헤더에 실어서 보냄
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequest req) {
        var user = userService.login(req);
        String token = jwtTokenProvider.generateToken(user.userId());
        return ResponseEntity.ok(Map.of("token", token, "user", user));
    }

    /**
     * 👤 4단계: 다른 사람 프로필 조회
     * - userId(로그인 아이디)로 조회
     * - 채팅/프로필 화면에서 쓰이는 API
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable String userId) {
        UserResponse res = userService.getUserProfile(userId);
        return ResponseEntity.ok(res);
    }
}
