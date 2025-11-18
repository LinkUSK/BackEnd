package com.example.demo.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

/**
 * Gmail API 호출용 Access Token을 Refresh Token으로 교환해 받는다.
 * (간단히 매번 요청. 필요하면 캐시 로직 추가 가능)
 */
@Service
public class GmailOAuthService {

    private final RestTemplate rest = new RestTemplate();

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;

    // (선택) 아주 단순 캐시
    private volatile String cachedAccessToken;
    private volatile Instant cachedExpiry = Instant.EPOCH;

    public GmailOAuthService(
            @Value("${gmail.oauth.client-id}") String clientId,
            @Value("${gmail.oauth.client-secret}") String clientSecret,
            @Value("${gmail.oauth.refresh-token}") String refreshToken
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }

    public synchronized String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(cachedExpiry.minusSeconds(30))) {
            return cachedAccessToken;
        }

        String url = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(body, headers);
        var res = rest.postForEntity(url, req, TokenResponse.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null || res.getBody().access_token == null) {
            throw new RuntimeException("Gmail OAuth 토큰 발급 실패: " + res.getStatusCode());
        }

        this.cachedAccessToken = res.getBody().access_token;
        // expires_in 보통 3600
        int expiresIn = res.getBody().expires_in != null ? res.getBody().expires_in : 3600;
        this.cachedExpiry = Instant.now().plusSeconds(expiresIn);

        return this.cachedAccessToken;
    }

    private static class TokenResponse {
        public String access_token;
        public Integer expires_in;
        public String token_type;
        public String scope;
    }
}
