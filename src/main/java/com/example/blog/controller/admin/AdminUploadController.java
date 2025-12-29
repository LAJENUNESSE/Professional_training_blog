package com.example.blog.controller.admin;

import com.example.blog.common.Result;
import com.example.blog.dto.response.UploadResponse;
import com.example.blog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class AdminUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public Result<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(fileStorageService.storeImage(file));
    }

    @PostMapping("/batch")
    public Result<List<UploadResponse>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        List<UploadResponse> responses = Stream.of(files)
                .map(fileStorageService::storeImage)
                .collect(Collectors.toList());
        return Result.success(responses);
    }
}
