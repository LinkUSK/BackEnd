// src/main/java/com/example/demo/controller/AiController.java
package com.example.demo.controller;

import com.example.demo.service.AiSearchService;
import com.example.demo.service.AiTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiTagService aiTagService;
    private final AiSearchService aiSearchService;

    /**
     * 현재 로그인한 사용자 전공을 가져오고 싶으면,
     * 필요 시 UserRepository를 주입해서 userId 로 조회해도 됨.
     * 지금은 간단히 principal 문자열만 꺼내는 유틸.
     */
    private String currentUserIdOrNull() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return (a == null || a.getPrincipal() == null) ? null : (String) a.getPrincipal();
    }

    /** 태그 자동 추천 */
    @PostMapping("/suggest-tags")
    public ResponseEntity<List<String>> suggestTags(@RequestBody Map<String, String> req) {
        String title = req.getOrDefault("title", "");
        String content = req.getOrDefault("content", "");
        String major = req.get("major"); // 프론트에서 me.major 보내주면 됨

        List<String> tags = aiTagService.suggestTags(title, content, major);
        return ResponseEntity.ok(tags);
    }

    /** 검색어/태그 추천 */
    @GetMapping("/search-suggest")
    public ResponseEntity<Map<String, Object>> searchSuggest(
            @RequestParam String q,
            @RequestParam(required = false) String major
    ) {
        Map<String, Object> res = aiSearchService.suggestSearch(q, major);
        return ResponseEntity.ok(res);
    }
}
