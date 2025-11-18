// src/main/java/com/example/demo/service/AiSearchService.java
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

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    /**
     * 검색어 + 전공을 바탕으로 추천 검색어 / 태그 추천.
     * 반환 형식: {"queries":[...], "tags":[...]}
     */
    public Map<String, Object> suggestSearch(String q, String major) {
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

        String raw = openAiClient.chat(prompt, 0.4);

        try {
            Map<String, Object> map = objectMapper.readValue(
                    raw,
                    new TypeReference<Map<String, Object>>() {}
            );
            // 최소 구조 보정
            if (!map.containsKey("queries")) map.put("queries", List.of());
            if (!map.containsKey("tags")) map.put("tags", List.of());
            return map;
        } catch (IOException e) {
            // 파싱 실패 시, 안전한 기본값
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("queries", List.of());
            fallback.put("tags", List.of());
            return fallback;
        }
    }
}
