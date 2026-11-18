package com.aman.hospital_management.controller;

import com.aman.hospital_management.dto.ConsultationResponse;
import com.aman.hospital_management.dto.ConsultationDocumentDownloadResponse;
import com.aman.hospital_management.model.Role;
import com.aman.hospital_management.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConsultationController {
    private final ConsultationService consultationService;

    @PostMapping("/appointments/{appointmentId}/consultation")
    public ResponseEntity<ConsultationResponse> startOrResume(Authentication authentication,
                                                               @PathVariable Long appointmentId) {
        return withVersion(consultationService.startOrResume(
                authentication.getName(), currentRole(authentication), appointmentId));
    }

    @GetMapping("/consultations/{consultationId}")
    public ResponseEntity<ConsultationResponse> get(Authentication authentication, @PathVariable Long consultationId) {
        return withVersion(consultationService.get(authentication.getName(), currentRole(authentication), consultationId));
    }

    @PutMapping("/consultations/{consultationId}/diagnosis")
    public ResponseEntity<ConsultationResponse> saveDiagnosis(Authentication authentication,
                                                               @PathVariable Long consultationId,
                                                               @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                               @RequestBody Map<String, Object> data) {
        return withVersion(consultationService.saveDiagnosis(authentication.getName(), currentRole(authentication),
                consultationId, data, parseVersion(ifMatch)));
    }

    @PutMapping("/consultations/{consultationId}/evaluation")
    public ResponseEntity<ConsultationResponse> saveEvaluation(Authentication authentication,
                                                                @PathVariable Long consultationId,
                                                                @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                                @RequestBody Map<String, Object> data) {
        return withVersion(consultationService.saveEvaluation(authentication.getName(), currentRole(authentication),
                consultationId, data, parseVersion(ifMatch)));
    }

    @PutMapping("/consultations/{consultationId}/care-plan")
    public ResponseEntity<ConsultationResponse> saveCarePlan(Authentication authentication,
                                                              @PathVariable Long consultationId,
                                                              @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                              @RequestBody Map<String, Object> data) {
        return withVersion(consultationService.saveCarePlan(authentication.getName(), currentRole(authentication),
                consultationId, data, parseVersion(ifMatch)));
    }

    @PutMapping("/consultations/{consultationId}/fees")
    public ResponseEntity<ConsultationResponse> saveFees(Authentication authentication,
                                                         @PathVariable Long consultationId,
                                                         @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                         @RequestBody Map<String, Object> data) {
        return withVersion(consultationService.saveFees(authentication.getName(), currentRole(authentication),
                consultationId, data, parseVersion(ifMatch)));
    }

    @PutMapping("/consultations/{consultationId}/prescription")
    public ResponseEntity<ConsultationResponse> savePrescription(Authentication authentication,
                                                                  @PathVariable Long consultationId,
                                                                  @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                                  @RequestBody Map<String, Object> data) {
        return withVersion(consultationService.savePrescription(authentication.getName(), currentRole(authentication),
                consultationId, data, parseVersion(ifMatch)));
    }

    @PostMapping("/consultations/{consultationId}/complete")
    public ResponseEntity<ConsultationResponse> complete(Authentication authentication,
                                         @PathVariable Long consultationId,
                                         @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        return withVersion(consultationService.complete(authentication.getName(), currentRole(authentication),
                consultationId, parseVersion(ifMatch)));
    }

    @PostMapping(value = "/consultations/{consultationId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConsultationResponse> uploadDocument(Authentication authentication,
                                               @PathVariable Long consultationId,
                                               @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                               @RequestPart("file") MultipartFile file) {
        return withVersion(consultationService.uploadDocument(authentication.getName(), currentRole(authentication),
                consultationId, file, parseVersion(ifMatch)));
    }

    @GetMapping("/consultations/{consultationId}/documents/{documentId}/download")
    public ConsultationDocumentDownloadResponse documentDownload(Authentication authentication,
                                                                  @PathVariable Long consultationId,
                                                                  @PathVariable String documentId) {
        return consultationService.documentDownload(authentication.getName(), currentRole(authentication),
                consultationId, documentId);
    }

    private Role currentRole(Authentication authentication) {
        return Role.valueOf(authentication.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                .map(authority -> authority.getAuthority().substring("ROLE_".length()))
                .findFirst().orElseThrow());
    }

    private ResponseEntity<ConsultationResponse> withVersion(ConsultationResponse response) {
        return ResponseEntity.ok().eTag("\"" + response.version() + "\"").body(response);
    }

    private long parseVersion(String ifMatch) {
        if (ifMatch == null) throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                "If-Match header is required.");
        String value = ifMatch.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "If-Match must contain the consultation version ETag.", ex);
        }
    }
}
