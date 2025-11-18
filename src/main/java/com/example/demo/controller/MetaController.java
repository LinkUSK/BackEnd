// com.example.demo.controller.MetaController.java
package com.example.demo.controller;

import com.example.demo.dto.meta.TagCreateRequest;
import com.example.demo.dto.meta.TagResponse;
import com.example.demo.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final TagService tagService;

    public MetaController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(tagService.getCategories());
    }

    @GetMapping("/tags")
    public ResponseEntity<List<TagResponse>> tags(@RequestParam String category) {
        return ResponseEntity.ok(tagService.getTagsByCategory(category));
    }

    @PostMapping("/tags")
    public ResponseEntity<TagResponse> create(@RequestBody TagCreateRequest req) {
        return ResponseEntity.ok(tagService.createOrReviveTag(req)); // ✅ 동일/비활성 존재 시 revive
    }

    // ✅ idempotent soft delete
    @DeleteMapping("/tags/{id}")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        tagService.deactivateTagIfExists(id);
        return ResponseEntity.noContent().build();
    }
}
