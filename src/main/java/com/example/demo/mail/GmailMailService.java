package com.example.demo.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Gmail API (users.messages.send) 로 인증코드 메일 발송
 */
@Service
public class GmailMailService {

    private final GmailOAuthService oAuthService;
    private final String senderAddress;
    private final RestTemplate rest = new RestTemplate();

    public GmailMailService(
            GmailOAuthService oAuthService,
            @Value("${gmail.sender.address}") String senderAddress
    ) {
        this.oAuthService = oAuthService;
        this.senderAddress = senderAddress;
    }

    public void sendVerificationCode(String to, String code) {
        try {
            String subject = encodeSubject("이메일 인증코드");
            String text = """
                    안녕하세요.
                    아래 인증코드를 10분 이내 입력해주세요.

                    인증코드: %s
                    """.formatted(code);

            String mime = buildMime(to, subject, text);

            String rawBase64Url = base64UrlEncode(mime.getBytes(StandardCharsets.UTF_8));

            String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(oAuthService.getAccessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of("raw", rawBase64Url);
            HttpEntity<Map<String, String>> req = new HttpEntity<>(body, headers);

            ResponseEntity<String> res = rest.postForEntity(url, req, String.class);
            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Gmail 발송 실패: HTTP " + res.getStatusCodeValue() + " - " + res.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("메일 발송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * MIME: 반드시 CRLF(\r\n) 사용을 권장
     */
    private String buildMime(String to, String subjectEncoded, String text) {
        // From 헤더는 Gmail이 토큰 소유자 계정으로 대체하지만, 명시해두는 편이 가독성에 좋다
        return "From: " + senderAddress + "\r\n" +
                "To: " + to + "\r\n" +
                "Subject: " + subjectEncoded + "\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "MIME-Version: 1.0\r\n" +
                "\r\n" +
                text;
    }

    /**
     * RFC 2047 encoded-word: =?UTF-8?B?<base64>?=
     * 한글 제목을 안전하게 인코딩
     */
    private String encodeSubject(String subjectUtf8) {
        String b64 = Base64.getEncoder().encodeToString(subjectUtf8.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + b64 + "?=";
    }

    /**
     * Gmail API는 Base64url(+,/ -> -,_ , padding 제거) 인코딩을 요구
     */
    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
