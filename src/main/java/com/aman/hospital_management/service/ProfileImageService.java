package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.ProfileImageResponse;
import com.aman.hospital_management.exception.UserNotFoundException;
import com.aman.hospital_management.model.AppUser;
import com.aman.hospital_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final UserRepository userRepository;
    private final FileService fileService;

    @Transactional
    public ProfileImageResponse upload(String email, MultipartFile image) {
        AppUser user = findUser(email);
        String previousPublicId = user.getProfileImagePublicId();
        Map<String, Object> uploaded = fileService.upload(image);
        Object secureUrl = uploaded.get("secure_url");
        Object publicId = uploaded.get("public_id");
        if (!(secureUrl instanceof String url) || !(publicId instanceof String id)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary did not return image details");
        }
        user.setProfileImageUrl(url);
        user.setProfileImagePublicId(id);
        userRepository.save(user);
        if (previousPublicId != null && !previousPublicId.equals(id)) {
            fileService.delete(previousPublicId);
        }
        return new ProfileImageResponse(url);
    }

    @Transactional
    public void delete(String email) {
        AppUser user = findUser(email);
        if (user.getProfileImagePublicId() == null) {
            return;
        }
        fileService.delete(user.getProfileImagePublicId());
        user.setProfileImageUrl(null);
        user.setProfileImagePublicId(null);
        userRepository.save(user);
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
