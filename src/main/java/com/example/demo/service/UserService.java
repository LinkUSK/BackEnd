package com.example.demo.service;

import com.example.demo.dto.UserLoginRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service // 유저 관련 비즈니스 로직을 담당하는 서비스
public class UserService {

    private final UserRepository userRepository;          // 유저 DB 접근용
    private final PasswordEncoder passwordEncoder;        // 비밀번호 암호화 도구
    private final VerificationService verificationService; // 이메일 인증 확인용

    // 생성자 주입 (스프링이 자동으로 넣어줌)
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       VerificationService verificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
    }

    // 유저 생성 날짜를 "yyyy-MM-dd HH:mm" 형태의 문자로 바꾸는 함수
    private String formatCreatedAt(User u) {
        if (u.getCreatedAt() == null) return null;
        return u.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * 로그인 기능
     * - 이메일 또는 userId 둘 다로 로그인 가능
     * - 비밀번호가 맞는지 확인 후 UserResponse 반환
     */
    @Transactional(readOnly = true)   // 조회만 하므로 readOnly
    public UserResponse login(UserLoginRequest req) {
        String key = req.userId().trim();  // 프론트에서는 여기에 이메일을 주로 넣음
        String reqPassword = req.password(); // 사용자가 입력한 비밀번호

        // 1) 이메일로 먼저 찾고
        // 2) 없으면 userId로 찾기
        User u = userRepository.findByEmail(key)
                .or(() -> userRepository.findByUserId(key))
                .orElseThrow(() ->
                        new IllegalArgumentException("아이디(이메일) 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호가 맞는지 확인 (암호화된 비밀번호와 비교)
        if (!passwordEncoder.matches(reqPassword, u.getPassword())) {
            throw new IllegalArgumentException("아이디(이메일) 또는 비밀번호가 올바르지 않습니다.");
        }

        // 로그인 성공 시, 프론트에 내려줄 값 포장
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getUserId(),
                u.getEmail(),
                u.getMajor(),
                u.getProfileImageUrl(),
                formatCreatedAt(u) // 가입일 포맷
        );
    }

    /**
     * 이메일 인증이 끝난 후 실제 회원을 만드는 함수
     * - 이메일 인증 여부 확인
     * - 이미 가입된 이메일인지 체크
     * - 이메일에서 userId 자동 생성
     */
    @Transactional
    public UserResponse createUserAfterEmailVerified(String email,
                                                     String username,
                                                     String rawPassword,
                                                     String major,
                                                     String profileImageUrl) {
        // 이메일, 이름 앞뒤 공백 제거 + 소문자 처리
        String e = email == null ? null : email.trim().toLowerCase();
        String n = username == null ? null : username.trim();

        // 필수 항목 체크
        if (e == null || n == null || rawPassword == null) {
            throw new IllegalArgumentException("회원가입 정보가 올바르지 않습니다.");
        }

        // 이메일 인증이 실제로 완료됐는지 VerificationService에 물어봄
        if (!verificationService.isVerified(e)) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        // 같은 이메일로 이미 가입한 사람이 있으면 에러
        if (userRepository.existsByEmail(e)) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        // 이메일 로컬파트(앞부분)로 userId 자동 생성
        String generatedUserId = generateUserIdFromEmail(e);

        // 새 User 엔티티 만들기
        User user = new User();
        user.setUsername(n);
        user.setUserId(generatedUserId);
        user.setEmail(e);
        // 비밀번호를 암호화해서 저장
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setMajor(major);
        user.setProfileImageUrl(profileImageUrl);

        // DB에 저장
        User saved = userRepository.save(user);

        // 가입된 유저 정보 응답 DTO 로 만들어 반환
        return new UserResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getUserId(),
                saved.getEmail(),
                saved.getMajor(),
                saved.getProfileImageUrl(),
                formatCreatedAt(saved)
        );
    }

    /**
     * 이메일의 @ 앞부분을 이용해 userId 자동 생성
     * - 최대 길이: 20자
     * - 이미 사용 중이면 -2, -3 ... 같은 숫자를 뒤에 붙임
     */
    private String generateUserIdFromEmail(String email) {
        // 이메일의 앞부분(local-part)만 가져옴
        String local = email.split("@")[0].toLowerCase()
                // 영문, 숫자, ., _, - 만 허용하고 나머지는 제거
                .replaceAll("[^a-z0-9._-]", "");
        if (local.isEmpty()) local = "user";

        // 20자 제한
        String base = local.length() > 20 ? local.substring(0, 20) : local;
        String candidate = base;
        int seq = 2;

        // 이미 같은 userId가 있으면 뒤에 -2, -3 등을 붙여 중복 피하기
        while (userRepository.existsByUserId(candidate)) {
            String suffix = "-" + seq;
            int limit = 20 - suffix.length();
            candidate = (base.length() > limit ? base.substring(0, limit) : base) + suffix;
            seq++;
        }
        return candidate;
    }

    // userId로 실제 User 엔티티 찾기 (내부에서 주로 사용)
    @Transactional(readOnly = true)
    public User getByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    // userId로 유저 삭제하기
    @Transactional
    public void deleteByUserId(String userId) {
        User u = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        userRepository.delete(u);
    }

    /**
     * 상대방 프로필 조회용
     * - userId로 검색해서 UserResponse 로 변환해 반환
     */
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
                formatCreatedAt(u)
        );
    }
}
