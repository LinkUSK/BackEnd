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

    @PostMapping("/request-code")
    public ResponseEntity<?> requestCode(@Valid @RequestBody RequestCodeRequest req) {
        verificationService.requestCode(req.email());
        return ResponseEntity.ok(Map.of("message", "인증코드를 발송했습니다."));
    }

    // ▼ 이메일 인증 후 회원가입
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyAndSignup(@Valid @RequestBody VerifyCodeRequest req) {
        verificationService.verifyAndConsume(req.email(), req.code());

        UserResponse created = userService.createUserAfterEmailVerified(
                req.email(), req.username(), req.password(), req.major(), req.profileImageUrl()
        );

        String token = jwtTokenProvider.generateToken(created.userId());
        return ResponseEntity.ok(Map.of("token", token, "user", created));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequest req) {
        var user = userService.login(req);
        String token = jwtTokenProvider.generateToken(user.userId());
        return ResponseEntity.ok(Map.of("token", token, "user", user));
    }

    // 🔹 상대방 프로필 조회 (userId 기준)
    // 예) GET /api/auth/users/kim-harin
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable String userId) {
        UserResponse res = userService.getUserProfile(userId);
        return ResponseEntity.ok(res);
    }
}
