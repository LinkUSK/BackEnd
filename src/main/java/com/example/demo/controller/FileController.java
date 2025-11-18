package com.example.demo.controller;

import com.example.demo.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileController {

    private final FileStorageService storage;
    public FileController(FileStorageService storage){ this.storage = storage; }

    @PostMapping(value="/api/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file){
        String url = storage.saveImage(file);
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }

    // ▼ 새로 추가: 비로그인 공개 업로드
    @PostMapping(value="/api/files/upload-public", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPublic(@RequestPart("file") MultipartFile file){
        String url = storage.saveImage(file);
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) {
        try {
            var path = storage.resolveOnDisk(filename);
            var res = new UrlResource(path.toUri());
            if (!res.exists()) return ResponseEntity.notFound().build();
            String contentType = Files.probeContentType(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                    .body(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
