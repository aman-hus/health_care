package com.aman.hospital_management.service;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.time.Instant;
import java.util.UUID;

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

    public Map<String, Object> uploadConsultationDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "A consultation document is required.");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "Document must be 5 MB or smaller.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !java.util.Set.of("application/pdf", "image/jpeg", "image/png", "image/webp")
                .contains(contentType.toLowerCase())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Only PDF, JPEG, PNG, and WEBP documents are supported.");
        }
        try {
            byte[] bytes = file.getBytes();
            String normalizedType = contentType.toLowerCase();
            validateDocumentSignature(normalizedType, bytes);
            Map<String, Object> result = cloudinary.uploader().upload(bytes, Map.of(
                    "resource_type", "raw",
                    "type", "authenticated",
                    "folder", "hospital-management/consultations",
                    "public_id", UUID.randomUUID() + "." + extensionFor(normalizedType)
            ));
            Object publicId = result.get("public_id");
            if (publicId == null) throw new IllegalStateException("Cloudinary did not return an asset identifier.");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("publicId", publicId);
            metadata.put("resourceType", result.getOrDefault("resource_type", "raw"));
            metadata.put("format", result.getOrDefault("format", extensionFor(normalizedType)));
            metadata.put("contentType", normalizedType);
            metadata.put("originalFilename", safeFilename(file.getOriginalFilename()));
            metadata.put("sizeBytes", file.getSize());
            metadata.put("uploadedAt", Instant.now().toString());
            return metadata;
        } catch (org.springframework.web.server.ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Could not upload consultation document.", e);
        }
    }

    public String consultationDocumentDownloadUrl(String publicId, String format) {
        try {
            long expiresAt = Instant.now().plusSeconds(300).getEpochSecond();
            return cloudinary.privateDownload(publicId, format, Map.of(
                    "resource_type", "raw",
                    "type", "authenticated",
                    "expires_at", expiresAt
            ));
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Could not create a secure document download link.", e);
        }
    }

    public void deleteConsultationDocument(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, Map.of(
                    "resource_type", "raw",
                    "type", "authenticated",
                    "invalidate", true
            ));
        } catch (IOException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Could not remove the consultation document from storage.", e);
        }
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "document";
        String normalized = filename.replace('\\', '/');
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1);
        return baseName.length() > 255 ? baseName.substring(baseName.length() - 255) : baseName;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> "pdf";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("Unsupported consultation document type.");
        };
    }

    private void validateDocumentSignature(String contentType, byte[] bytes) {
        boolean valid = switch (contentType) {
            case "application/pdf" -> bytes.length >= 5
                    && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-';
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                    && (bytes[4] & 0xff) == 0x0d && (bytes[5] & 0xff) == 0x0a
                    && (bytes[6] & 0xff) == 0x1a && (bytes[7] & 0xff) == 0x0a;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
        if (!valid) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Document contents do not match the declared file type.");
        }
    }
}
