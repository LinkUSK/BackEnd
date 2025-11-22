// src/main/java/com/example/demo/service/FileStorageService.java
package com.example.demo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 이미지 파일을 Cloudinary에 업로드하고,
 * 업로드된 파일의 HTTPS URL(secure_url)을 반환하는 서비스.
 */
@Service
public class FileStorageService {

    private final Cloudinary cloudinary;

    public FileStorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    /**
     * 이미지를 Cloudinary에 업로드하고, HTTPS URL을 반환한다.
     *
     * @param file 업로드할 이미지 파일
     * @return 업로드된 이미지의 secure_url (예: https://res.cloudinary.com/...)
     */
    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일입니다.");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image"
                            // 필요하면 폴더 설정도 가능:
                            // "folder", "linku/profile"
                    )
            );

            Object url = uploadResult.get("secure_url");
            if (url == null) {
                throw new RuntimeException("Cloudinary 응답에 secure_url이 없습니다.");
            }

            return url.toString(); // Cloudinary 절대 URL
        } catch (Exception e) {
            throw new RuntimeException("이미지 업로드 실패", e);
        }
    }
}
