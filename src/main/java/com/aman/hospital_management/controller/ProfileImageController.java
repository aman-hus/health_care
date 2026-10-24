package com.aman.hospital_management.controller;

import com.aman.hospital_management.dto.ProfileImageResponse;
import com.aman.hospital_management.service.ProfileImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me/photo")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileImageResponse upload(Authentication authentication,
                                       @RequestPart("image") MultipartFile image) {
        return profileImageService.upload(authentication.getName(), image);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(Authentication authentication) {
        profileImageService.delete(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
