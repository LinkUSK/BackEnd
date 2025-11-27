// 이메일 인증 관련 기능만 담당하는 서비스 클래스
package com.example.demo.service;

import com.example.demo.mail.GmailMailService;
import com.example.demo.util.SchoolEmailValidator;
import com.example.demo.verification.EmailVerification;
import com.example.demo.verification.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 이메일 인증 전담 서비스
 *  - 인증코드 만들기
 *  - DB에 저장하기
 *  - Gmail API로 메일 보내기
 *  - 코드가 맞는지 / 만료됐는지 확인하기
 */
@Service                    // 스프링이 이 클래스를 서비스로 관리하게 해줌
@RequiredArgsConstructor    // final 필드를 자동으로 생성자에 넣어줌
public class VerificationService {

    // 이메일 인증 정보를 저장/조회하는 DB 레포지토리
    private final EmailVerificationRepository repository;
    // 실제 Gmail API를 사용해서 메일을 보내는 서비스
    private final GmailMailService mailService;
    // 학교 이메일(@skuniv.ac.kr)인지 체크하는 도우미
    private final SchoolEmailValidator validator;

    // 안전한 랜덤 숫자를 뽑기 위한 도구
    private static final SecureRandom RND = new SecureRandom();

    /**
     * 인증코드 보내기 요청
     * 1) 이메일이 학교 이메일인지 확인
     * 2) 6자리 랜덤 숫자 코드 만들기
     * 3) DB에 저장 (10분 뒤에 만료되도록)
     * 4) Gmail API로 실제 메일 보내기
     */
    public void requestCode(String email) {
        // 학교 이메일이 아니면 에러 발생
        if (!validator.isSchoolEmail(email)) {
            throw new IllegalArgumentException("학교 이메일(@skuniv.ac.kr)만 허용됩니다.");
        }

        // 000000 ~ 999999 사이의 6자리 코드 생성
        String code = String.format("%06d", RND.nextInt(1_000_000));

        // 새 인증코드 엔티티 만들기
        var ev = EmailVerification.builder()
                .email(email)                                 // 어떤 이메일인지
                .code(code)                                  // 방금 만든 코드
                .expiresAt(LocalDateTime.now().plusMinutes(10)) // 지금 시간 + 10분
                .verified(false)                             // 아직 인증 안 됨
                .build();

        // DB에 저장
        repository.save(ev);

        // Gmail API를 이용해 실제 메일 보내기
        mailService.sendVerificationCode(email, code);
    }

    /**
     * 사용자가 입력한 코드가 맞는지 확인 + 사용처리까지 하기
     * - 가장 최근에 만든 코드만 인정
     * - 이미 사용했거나, 시간이 지나면(만료) 에러
     * - 코드가 맞으면 verified=true 로 바꿔서 재사용 못 하게 막음
     */
    public void verifyAndConsume(String email, String code) {
        // 이 이메일로 가장 최근에 만든 인증코드 찾기
        var latest = repository.findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청 내역이 없습니다."));

        // 이미 이 코드로 인증을 한 적이 있으면
        if (latest.isVerified()) throw new IllegalStateException("이미 인증 완료된 코드입니다.");

        // 만료 시간(expiresAt)이 현재 시간보다 전이면, 시간 지나서 사용 불가
        if (latest.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("코드가 만료되었습니다.");

        // DB에 저장된 코드와 사용자가 입력한 코드가 다르면 에러
        if (!latest.getCode().equals(code))
            throw new IllegalArgumentException("코드가 일치하지 않습니다.");

        // 여기까지 통과했다는 것은 "정상적인 코드"라는 뜻 → 사용 완료 표시
        latest.setVerified(true);
        repository.save(latest); // 변경 내용 다시 저장
    }

    /**
     * 이 이메일이 현재 인증 완료 상태인지 간단히 확인하는 메서드
     * - 회원가입 로직에서 "진짜로 인증했는지" 체크할 때 사용
     */
    public boolean isVerified(String email) {
        return repository.findTopByEmailOrderByIdDesc(email)
                .map(EmailVerification::isVerified)   // 가장 최근 코드의 verified 값 꺼내기
                .orElse(false);                       // 기록이 없으면 false (인증 안됨)
    }
}
