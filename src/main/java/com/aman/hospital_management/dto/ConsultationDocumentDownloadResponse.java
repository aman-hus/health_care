package com.aman.hospital_management.dto;

public record ConsultationDocumentDownloadResponse(String documentId, String downloadUrl, int expiresInSeconds) { }
