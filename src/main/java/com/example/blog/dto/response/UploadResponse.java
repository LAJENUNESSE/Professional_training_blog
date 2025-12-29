package com.example.blog.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResponse {

    private String url;
    private String filename;
    private String originalFilename;
    private Long size;
    private String contentType;

    public static UploadResponse of(String url, String filename, String originalFilename, Long size, String contentType) {
        return UploadResponse.builder()
                .url(url)
                .filename(filename)
                .originalFilename(originalFilename)
                .size(size)
                .contentType(contentType)
                .build();
    }
}
