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

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    /**
     * 제목/내용/전공을 기반으로 태그 추천.
     * 결과 예: ["웹 개발","디자인","포트폴리오"]
     */
    public List<String> suggestTags(String title, String content, String major) {

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

        // 🔍 디버깅 로그
        System.out.println("🔥 GPT RAW TAG RESPONSE = " + raw);

        // JSON 배열 부분만 강제 추출
        String jsonOnly = extractJsonArray(raw);

        // 1차 파싱 시도
        try {
            List<String> arr = objectMapper.readValue(
                    jsonOnly,
                    new TypeReference<List<String>>() {}
            );
            return cleanTags(arr);
        } catch (IOException e) {
            System.out.println("⚠️ JSON parsing failed, fallback mode");
        }

        // 2차 fallback 파싱
        return cleanTags(fallbackParse(jsonOnly));
    }

    /**
     * GPT 응답에서 [ ... ] JSON 배열 부분만 추출
     * 여분의 설명, 줄바꿈이 있어도 처리됨
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

    /** JSON 파싱 실패 시 대체 로직 */
    private List<String> fallbackParse(String raw) {
        if (raw == null) return List.of();

        String s = raw.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);

        String[] parts = s.split("[,\n]");
        List<String> out = new ArrayList<>();

        for (String p : parts) {
            String t = p.trim();
            t = t.replaceAll("^\"|\"$", ""); // 양끝 따옴표 제거
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    /** 태그 정리 */
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

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
