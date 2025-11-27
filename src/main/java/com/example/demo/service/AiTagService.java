// AI에게 "이 글에 어울리는 태그"를 추천받는 서비스
package com.example.demo.service;

import com.example.demo.ai.OpenAiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiTagService {

    private final OpenAiClient openAiClient;   // GPT 호출용
    private final ObjectMapper objectMapper;   // JSON 파싱용

    /**
     * 제목 / 내용 / 전공을 기반으로
     * - 태그 문자열 리스트를 추천받음
     * 예: ["웹 개발","디자인","포트폴리오"]
     */
    public List<String> suggestTags(String title, String content, String major) {

        // GPT에게 보낼 프롬프트 (규칙을 아주 자세히 적어줌)
        String prompt = """
                너는 태그 추천 AI야.
                반드시 JSON 배열만 출력해.

                규칙:
                - 예: ["웹 개발","디자인","포트폴리오"]
                - 절대 JSON 외 다른 말 하지 마
                - 태그는 3개
                - 한글 태그만
                - 태그 길이 1~10자
                - 해시태그(#) 금지
                - 설명 쓰지 말 것

                입력:
                제목: %s
                내용: %s
                전공: %s
                """
                .formatted(
                        nullToEmpty(title),
                        nullToEmpty(content),
                        (major == null || major.isBlank()) ? "모름" : major
                );

        // GPT 호출
        String raw = openAiClient.chat(prompt, 0.3);

        // 디버깅용 로그
        System.out.println("🔥 GPT RAW TAG RESPONSE = " + raw);

        // 응답 문자열에서 JSON 배열 부분만 뽑기
        String jsonOnly = extractJsonArray(raw);

        // 1차: ObjectMapper를 이용해서 파싱 시도
        try {
            List<String> arr = objectMapper.readValue(
                    jsonOnly,
                    new TypeReference<List<String>>() {}
            );
            return cleanTags(arr);
        } catch (IOException e) {
            System.out.println("⚠️ JSON parsing failed, fallback mode");
        }

        // 2차: 직접 문자열을 쪼개서 파싱 (fallback)
        return cleanTags(fallbackParse(jsonOnly));
    }

    /**
     * GPT 응답 문자열에서 [ ... ] 부분만 잘라내기
     * - 앞뒤에 설명이 있어도 괜찮게 처리
     */
    private String extractJsonArray(String raw) {
        if (raw == null) return "[]";

        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');

        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return "[]";
    }

    /**
     * JSON 파싱 실패 시, 아주 단순하게 문자열을 분리하는 방식
     */
    private List<String> fallbackParse(String raw) {
        if (raw == null) return List.of();

        String s = raw.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);

        String[] parts = s.split("[,\n]");
        List<String> out = new ArrayList<>();

        for (String p : parts) {
            String t = p.trim();
            // 양 끝의 " 제거
            t = t.replaceAll("^\"|\"$", "");
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * 태그 문자열들을 정리하는 함수
     * - 공백 제거
     * - '#' 제거
     * - 중복 제거
     * - 최대 10개까지만 사용
     */
    private List<String> cleanTags(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (s == null) continue;
            String t = s.trim();
            if (t.startsWith("#")) t = t.substring(1).trim();
            if (!t.isEmpty() && !out.contains(t) && out.size() < 10) {
                out.add(t);
            }
        }
        return out;
    }

    // null 을 "" 로 바꿔주는 작은 도우미
    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
