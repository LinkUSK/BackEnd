// AI에게 "추천 검색어/태그"를 물어보는 서비스
package com.example.demo.service;

import com.example.demo.ai.OpenAiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiSearchService {

    private final OpenAiClient openAiClient;   // 실제 GPT와 통신하는 클라이언트
    private final ObjectMapper objectMapper;   // JSON 문자열을 Map으로 바꾸는 도구

    /**
     * 검색어(q) + 전공(major)를 바탕으로
     * - 추천 검색어 리스트
     * - 추천 태그 리스트
     * 를 AI에게 물어봄.
     * 결과 형태 예:
     * {
     *   "queries": ["웹 개발 포트폴리오","프론트엔드 튜터"],
     *   "tags": ["웹 개발","포트폴리오","튜터링"]
     * }
     */
    public Map<String, Object> suggestSearch(String q, String major) {
        // GPT에게 보낼 프롬프트 문자열 만들기
        String prompt = """
                너는 재능 공유 서비스의 검색어 추천 도우미야.
                사용자가 입력한 검색 문장과 전공을 보고,
                사용자가 관심 있어할 만한 '추천 검색어'와 '추천 태그 이름'을 제안해줘.

                규칙:
                - 반드시 JSON 객체 하나만 출력해.
                - 예시:
                  {
                    "queries": ["웹 개발 포트폴리오","프론트엔드 튜터"],
                    "tags": ["웹 개발","포트폴리오","튜터링"]
                  }
                - queries: 2~4개, 각 5~20자
                - tags: 3~6개, 태그 이름만 (해시태그 없이)

                현재 검색어: %s
                사용자의 전공/관심 분야: %s
                """.formatted(
                q == null ? "" : q,
                (major == null || major.isBlank()) ? "모름" : major
        );

        // GPT에게 질문 보내고, 응답(JSON 문자열)을 받음
        String raw = openAiClient.chat(prompt, 0.4);

        try {
            // JSON 문자열을 Map<String, Object> 형태로 변환
            Map<String, Object> map = objectMapper.readValue(
                    raw,
                    new TypeReference<Map<String, Object>>() {}
            );
            // 안전 장치: queries / tags 키가 없으면 빈 리스트로 채움
            if (!map.containsKey("queries")) map.put("queries", List.of());
            if (!map.containsKey("tags")) map.put("tags", List.of());
            return map;
        } catch (IOException e) {
            // JSON 파싱에 실패하면, 기본값(빈 리스트) 반환
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("queries", List.of());
            fallback.put("tags", List.of());
            return fallback;
        }
    }
}
