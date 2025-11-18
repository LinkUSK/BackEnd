package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String dir) throws IOException {
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public String saveImage(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("빈 파일입니다.");
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String name = UUID.randomUUID().toString().replace("-", "");
        String filename = (ext == null || ext.isBlank()) ? name : (name + "." + ext.toLowerCase());
        Path target = uploadDir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
        return "/files/" + filename;
    }

    public Path resolveOnDisk(String filename){
        return uploadDir.resolve(filename).normalize();
    }
}
