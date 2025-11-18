package com.example.demo.service;

import com.example.demo.mail.GmailMailService;
import com.example.demo.util.SchoolEmailValidator;
import com.example.demo.verification.EmailVerification;
import com.example.demo.verification.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final EmailVerificationRepository repository;
    private final GmailMailService mailService;       // ✅ Gmail API 사용
    private final SchoolEmailValidator validator;

    private static final SecureRandom RND = new SecureRandom();

    public void requestCode(String email) {
        if (!validator.isSchoolEmail(email)) {
            throw new IllegalArgumentException("학교 이메일(@skuniv.ac.kr)만 허용됩니다.");
        }
        String code = String.format("%06d", RND.nextInt(1_000_000));

        var ev = EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .build();
        repository.save(ev);

        // ✅ 실제 전송 (Gmail API)
        mailService.sendVerificationCode(email, code);
    }

    public void verifyAndConsume(String email, String code) {
        var latest = repository.findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청 내역이 없습니다."));
        if (latest.isVerified()) throw new IllegalStateException("이미 인증 완료된 코드입니다.");
        if (latest.getExpiresAt().isBefore(LocalDateTime.now())) throw new IllegalArgumentException("코드가 만료되었습니다.");
        if (!latest.getCode().equals(code)) throw new IllegalArgumentException("코드가 일치하지 않습니다.");

        latest.setVerified(true);
        repository.save(latest);
    }

    public boolean isVerified(String email) {
        return repository.findTopByEmailOrderByIdDesc(email)
                .map(EmailVerification::isVerified)
                .orElse(false);
    }
}
