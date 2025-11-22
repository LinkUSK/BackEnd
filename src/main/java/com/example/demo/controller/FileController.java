// src/main/java/com/example/demo/controller/FileController.java
package com.example.demo.controller;

import com.example.demo.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileController {

    private final FileStorageService storage;

    public FileController(FileStorageService storage) {
        this.storage = storage;
    }

    /**
     * 로그인한 사용자가 사용하는 일반 업로드
     */
    @PostMapping(
            value = "/api/files/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file) {
        String url = storage.saveImage(file);  // Cloudinary URL
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }

    /**
     * 비로그인 공개 업로드 (회원가입 등)
     */
    @PostMapping(
            value = "/api/files/upload-public",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadPublic(@RequestPart("file") MultipartFile file) {
        String url = storage.saveImage(file);  // Cloudinary URL
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }

    // ✅ 이전의 GET /files/{filename} 엔드포인트는 더 이상 필요 없음.
    //    새로 업로드되는 파일은 모두 Cloudinary URL을 사용하므로
    //    백엔드에서 정적 이미지 파일을 서빙할 필요가 없다.
}
