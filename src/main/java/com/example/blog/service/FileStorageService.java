package com.example.blog.service;

import com.example.blog.dto.response.UploadResponse;
import com.example.blog.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:/uploads}")
    private String baseUrl;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public UploadResponse storeImage(MultipartFile file) {
        validateImage(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = generateFilename(extension);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));

        try {
            Path targetDir = Paths.get(uploadDir, datePath);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String url = baseUrl + "/" + datePath + "/" + filename;

            return UploadResponse.of(
                    url,
                    filename,
                    originalFilename,
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            throw BusinessException.badRequest("Failed to store file: " + e.getMessage());
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest("File size exceeds maximum allowed size (10MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw BusinessException.badRequest("Only image files are allowed (JPEG, PNG, GIF, WebP, SVG)");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String generateFilename(String extension) {
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
}
