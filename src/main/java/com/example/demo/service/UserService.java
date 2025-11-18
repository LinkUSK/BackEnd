package com.example.demo.service;

import com.example.demo.dto.UserLoginRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       VerificationService verificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
    }

    private String formatCreatedAt(User u) {
        if (u.getCreatedAt() == null) return null;
        return u.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /** 이메일 또는 userId 로 로그인 허용 */
    @Transactional(readOnly = true)
    public UserResponse login(UserLoginRequest req) {
        String key = req.userId().trim();     // 프론트는 여기다 "이메일"을 보냄
        String reqPassword = req.password();

        // 이메일 우선, 없으면 userId 로 조회
        User u = userRepository.findByEmail(key)
                .or(() -> userRepository.findByUserId(key))
                .orElseThrow(() -> new IllegalArgumentException("아이디(이메일) 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(reqPassword, u.getPassword())) {
            throw new IllegalArgumentException("아이디(이메일) 또는 비밀번호가 올바르지 않습니다.");
        }
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getUserId(),
                u.getEmail(),
                u.getMajor(),
                u.getProfileImageUrl(),
                formatCreatedAt(u) // ✅
        );
    }

    /** 이메일 인증 완료 후 최종 가입 (userId 자동 생성) */
    @Transactional
    public UserResponse createUserAfterEmailVerified(String email,
                                                     String username,
                                                     String rawPassword,
                                                     String major,
                                                     String profileImageUrl) {
        String e = email == null ? null : email.trim().toLowerCase();
        String n = username == null ? null : username.trim();

        if (e == null || n == null || rawPassword == null) {
            throw new IllegalArgumentException("회원가입 정보가 올바르지 않습니다.");
        }
        if (!verificationService.isVerified(e)) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }
        if (userRepository.existsByEmail(e)) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        // userId 자동 생성 (이메일 로컬파트 기반, 20자 제한, 중복 시 suffix)
        String generatedUserId = generateUserIdFromEmail(e);

        User user = new User();
        user.setUsername(n);
        user.setUserId(generatedUserId);
        user.setEmail(e);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setMajor(major);
        user.setProfileImageUrl(profileImageUrl);

        User saved = userRepository.save(user);
        return new UserResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getUserId(),
                saved.getEmail(),
                saved.getMajor(),
                saved.getProfileImageUrl(),
                formatCreatedAt(saved)  // ✅
        );
    }

    /** 이메일 로컬파트로 userId 생성 (최대 20자, 중복 시 -2, -3 …) */
    private String generateUserIdFromEmail(String email) {
        String local = email.split("@")[0].toLowerCase()
                .replaceAll("[^a-z0-9._-]", ""); // 안전 문자만
        if (local.isEmpty()) local = "user";

        // 20자 제한
        String base = local.length() > 20 ? local.substring(0, 20) : local;
        String candidate = base;
        int seq = 2;
        while (userRepository.existsByUserId(candidate)) {
            String suffix = "-" + seq;
            int limit = 20 - suffix.length();
            candidate = (base.length() > limit ? base.substring(0, limit) : base) + suffix;
            seq++;
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public User getByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public void deleteByUserId(String userId) {
        User u = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        userRepository.delete(u);
    }

    // 🔹 상대방 프로필 조회용 (userId -> UserResponse)
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String userId) {
        User u = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getUserId(),
                u.getEmail(),
                u.getMajor(),
                u.getProfileImageUrl(),
                formatCreatedAt(u) // ✅
        );
    }
}
