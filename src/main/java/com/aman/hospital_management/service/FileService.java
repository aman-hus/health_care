package com.aman.hospital_management.service;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

@Service
public class FileService {
    @Autowired
    private Cloudinary cloudinary;

    public Map<String, Object> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !java.util.Set.of("image/jpeg", "image/png", "image/webp", "image/gif")
                .contains(contentType.toLowerCase())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Only JPEG, PNG, WEBP, and GIF images are allowed");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "Image must be 5 MB or smaller");
        }
        try {
            Map<String, Object> data = this.cloudinary.uploader().upload(file.getBytes(), Map.of(
                    "resource_type", "image", "folder", "hospital-management/profiles"));
            return data;
        } catch (IOException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY, "Could not upload image to Cloudinary", e);
        }
    }

    public void delete(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, Map.of("invalidate", true));
            if (!"ok".equals(result.get("result")) && !"not found".equals(result.get("result"))) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_GATEWAY, "Could not remove image from Cloudinary");
            }
        } catch (IOException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY, "Could not remove image from Cloudinary", e);
        }
    }
}
