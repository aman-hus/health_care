package com.aman.hospital_management.service;

import com.aman.hospital_management.dto.ConsultationDocumentDownloadResponse;
import com.aman.hospital_management.dto.ConsultationResponse;
import com.aman.hospital_management.model.Appointment;
import com.aman.hospital_management.model.AppointmentStatus;
import com.aman.hospital_management.model.Consultation;
import com.aman.hospital_management.model.ConsultationSection;
import com.aman.hospital_management.model.ConsultationStatus;
import com.aman.hospital_management.model.Role;
import com.aman.hospital_management.repository.AppointmentRepository;
import com.aman.hospital_management.repository.ConsultationRepository;
import com.aman.hospital_management.security.HospitalContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultationService {
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final FileService fileService;

    @Transactional
    public ConsultationResponse startOrResume(String email, Role callerRole, Long appointmentId) {
        requireDoctor(callerRole);
        Long hospitalId = HospitalContext.requireHospitalId();
        Appointment appointment = appointmentRepository.findByIdAndHospitalIdForUpdate(appointmentId, hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found."));
        requireAssignedDoctor(appointment, email);

        Consultation consultation = consultationRepository.findByAppointment_IdAndHospital_Id(appointmentId, hospitalId)
                .orElse(null);
        if (consultation == null) {
            if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Only an accepted appointment can start a consultation.");
            }
            consultation = consultationRepository.save(new Consultation(appointment));
        }
        return ConsultationResponse.from(consultation);
    }

    @Transactional(readOnly = true)
    public ConsultationResponse get(String email, Role callerRole, Long consultationId) {
        requireDoctor(callerRole);
        Consultation consultation = consultationInCurrentHospital(consultationId);
        requireAssignedDoctor(consultation, email);
        return ConsultationResponse.from(consultation);
    }

    @Transactional
    public ConsultationResponse saveDiagnosis(String email, Role role, Long id,
                                               Map<String, Object> data, long version) {
        return saveSection(email, role, id, ConsultationSection.DIAGNOSIS, data, version);
    }

    @Transactional
    public ConsultationResponse saveEvaluation(String email, Role role, Long id,
                                                Map<String, Object> data, long version) {
        return saveSection(email, role, id, ConsultationSection.EVALUATION, data, version);
    }

    @Transactional
    public ConsultationResponse saveCarePlan(String email, Role role, Long id,
                                              Map<String, Object> data, long version) {
        return saveSection(email, role, id, ConsultationSection.CARE_PLAN, data, version);
    }

    @Transactional
    public ConsultationResponse saveFees(String email, Role role, Long id,
                                         Map<String, Object> data, long version) {
        return saveSection(email, role, id, ConsultationSection.FEES, data, version);
    }

    @Transactional
    public ConsultationResponse savePrescription(String email, Role role, Long id,
                                                  Map<String, Object> data, long version) {
        return saveSection(email, role, id, ConsultationSection.PRESCRIPTION, data, version);
    }

    private ConsultationResponse saveSection(String email, Role callerRole, Long consultationId,
                                             ConsultationSection section, Map<String, Object> data,
                                             long expectedVersion) {
        requireDoctor(callerRole);
        Consultation consultation = consultationInCurrentHospital(consultationId);
        requireAssignedDoctor(consultation, email);
        requireEditableVersion(consultation, expectedVersion);

        ConsultationSection previousSection = section.previous();
        if (previousSection != null) {
            Map<String, Object> previous = sectionData(consultation, previousSection);
            if (previous == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Save the previous consultation section before continuing.");
            }
            validateSection(previousSection, previous);
        }

        if (data == null) throw badRequest("Section data must be a JSON object.");
        Map<String, Object> sectionInput = deepCopyMap(data);
        Map<String, Object> existingDiagnosis = consultation.getDiagnosisData();
        if (section == ConsultationSection.DIAGNOSIS) {
            sectionInput.remove("documents"); // Documents are modified only by the upload route.
        }

        Map<String, Object> validatedData = validateSection(section, sectionInput);
        if (section == ConsultationSection.DIAGNOSIS && existingDiagnosis != null
                && existingDiagnosis.get("documents") instanceof List<?> documents) {
            validatedData.put("documents", deepCopyList(documents));
        }

        try {
            switch (section) {
                case DIAGNOSIS -> consultation.saveDiagnosis(validatedData);
                case EVALUATION -> consultation.saveEvaluation(validatedData);
                case CARE_PLAN -> consultation.saveCarePlan(validatedData);
                case FEES -> consultation.saveFees(validatedData);
                case PRESCRIPTION -> consultation.savePrescription(validatedData);
            }
            consultationRepository.saveAndFlush(consultation);
            return ConsultationResponse.from(consultation);
        } catch (OptimisticLockingFailureException ex) {
            throw versionConflict(ex);
        }
    }

    @Transactional
    public ConsultationResponse complete(String email, Role callerRole, Long consultationId, long expectedVersion) {
        requireDoctor(callerRole);
        Consultation consultation = consultationInCurrentHospital(consultationId);
        requireAssignedDoctor(consultation, email);
        if (consultation.getVersion() != expectedVersion) throw versionConflict();
        requireAllSectionsSaved(consultation);
        if (consultation.getStatus() == ConsultationStatus.COMPLETED) return ConsultationResponse.from(consultation);

        try {
            consultation.complete();
            consultation.getAppointment().complete();
            consultationRepository.saveAndFlush(consultation);
            return ConsultationResponse.from(consultation);
        } catch (OptimisticLockingFailureException ex) {
            throw versionConflict(ex);
        }
    }

    @Transactional
    public ConsultationResponse uploadDocument(String email, Role callerRole, Long consultationId,
                                               MultipartFile file, long expectedVersion) {
        requireDoctor(callerRole);
        Consultation consultation = consultationInCurrentHospital(consultationId);
        requireAssignedDoctor(consultation, email);
        requireEditableVersion(consultation, expectedVersion);

        Map<String, Object> uploaded = fileService.uploadConsultationDocument(file);
        try {
            Map<String, Object> diagnosis = consultation.getDiagnosisData() == null
                    ? new LinkedHashMap<>() : consultation.getDiagnosisData();
            diagnosis.putIfAbsent("schemaVersion", 1);
            Object documentsValue = diagnosis.get("documents");
            List<Object> documents;
            if (documentsValue == null) {
                documents = new ArrayList<>();
                diagnosis.put("documents", documents);
            } else if (documentsValue instanceof List<?> existingDocuments) {
                documents = deepCopyList(existingDocuments);
                diagnosis.put("documents", documents);
            } else {
                throw badRequest("Stored documents value is invalid.");
            }

            Map<String, Object> document = new LinkedHashMap<>();
            document.put("id", UUID.randomUUID().toString());
            document.put("storageProvider", "CLOUDINARY");
            document.put("publicId", String.valueOf(uploaded.get("publicId")));
            document.put("resourceType", String.valueOf(uploaded.get("resourceType")));
            document.put("format", String.valueOf(uploaded.get("format")));
            document.put("contentType", String.valueOf(uploaded.get("contentType")));
            document.put("originalFilename", String.valueOf(uploaded.get("originalFilename")));
            document.put("sizeBytes", ((Number) uploaded.get("sizeBytes")).longValue());
            document.put("uploadedAt", String.valueOf(uploaded.get("uploadedAt")));
            documents.add(document);

            consultation.attachDiagnosisDocument(diagnosis);
            consultationRepository.saveAndFlush(consultation);
            return ConsultationResponse.from(consultation);
        } catch (RuntimeException ex) {
            try {
                fileService.deleteConsultationDocument(String.valueOf(uploaded.get("publicId")));
            } catch (RuntimeException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            if (ex instanceof OptimisticLockingFailureException) throw versionConflict(ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ConsultationDocumentDownloadResponse documentDownload(String email, Role callerRole,
                                                                  Long consultationId, String documentId) {
        requireDoctor(callerRole);
        Consultation consultation = consultationInCurrentHospital(consultationId);
        requireAssignedDoctor(consultation, email);
        Map<String, Object> diagnosis = consultation.getDiagnosisData();
        Object documentsValue = diagnosis == null ? null : diagnosis.get("documents");
        if (!(documentsValue instanceof List<?> documents)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation document not found.");
        }
        for (Object value : documents) {
            if (!(value instanceof Map<?, ?> document)) continue;
            if (!documentId.equals(String.valueOf(document.get("id")))) continue;
            String publicId = String.valueOf(document.get("publicId"));
            String resourceType = String.valueOf(document.get("resourceType"));
            String format = String.valueOf(document.get("format"));
            if (publicId.isBlank() || !"raw".equals(resourceType) || format.isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Consultation document metadata is incomplete.");
            }
            return new ConsultationDocumentDownloadResponse(documentId,
                    fileService.consultationDocumentDownloadUrl(publicId, format), 300);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation document not found.");
    }

    private Consultation consultationInCurrentHospital(Long id) {
        return consultationRepository.findByIdAndHospital_Id(id, HospitalContext.requireHospitalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found."));
    }

    private void requireDoctor(Role role) {
        if (role != Role.DOCTOR) throw new AccessDeniedException("Only doctors can access consultation workflows.");
    }

    private void requireAssignedDoctor(Appointment appointment, String email) {
        if (!appointment.getDoctor().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("Only the assigned doctor can access this consultation.");
        }
    }

    private void requireAssignedDoctor(Consultation consultation, String email) {
        if (!consultation.getDoctor().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("Only the assigned doctor can access this consultation.");
        }
    }

    private Map<String, Object> sectionData(Consultation consultation, ConsultationSection section) {
        return switch (section) {
            case DIAGNOSIS -> consultation.getDiagnosisData();
            case EVALUATION -> consultation.getEvaluationData();
            case CARE_PLAN -> consultation.getCarePlanData();
            case FEES -> consultation.getFeesData();
            case PRESCRIPTION -> consultation.getPrescriptionData();
        };
    }

    private Map<String, Object> validateSection(ConsultationSection section, Map<String, Object> rawData) {
        if (rawData == null) throw badRequest("Section data must be a JSON object.");
        Map<String, Object> payload = deepCopyMap(rawData);
        payload.put("schemaVersion", 1);
        switch (section) {
            case DIAGNOSIS -> {
                requireText(payload, "primaryDiagnosis");
                String type = requireText(payload, "diagnosisType").toUpperCase(Locale.ROOT);
                if (!Set.of("SUSPECTED", "CONFIRMED").contains(type)) {
                    throw badRequest("diagnosisType must be SUSPECTED or CONFIRMED.");
                }
                payload.put("diagnosisType", type);
                String severity = requireText(payload, "severity").toUpperCase(Locale.ROOT);
                if (!Set.of("MILD", "MODERATE", "SEVERE", "CRITICAL").contains(severity)) {
                    throw badRequest("severity must be MILD, MODERATE, SEVERE, or CRITICAL.");
                }
                payload.put("severity", severity);
                requireText(payload, "symptoms");
                optionalText(payload, "medicalHistoryContext");
                optionalText(payload, "assessmentNotes");
                optionalArray(payload, "documents");
            }
            case EVALUATION -> {
                optionalText(payload, "generalAppearance");
                optionalText(payload, "systemicFindings");
                requireText(payload, "examinationNotes");
                optionalText(payload, "additionalTests");
                validateVitals(payload);
            }
            case CARE_PLAN -> {
                requireText(payload, "treatmentSummary");
                optionalText(payload, "medicationGuidance");
                optionalText(payload, "lifestyleGuidance");
                optionalText(payload, "followUpNotes");
                optionalText(payload, "investigations");
                optionalText(payload, "warningSigns");
                Object days = payload.get("followUpDays");
                if (days != null && (!(days instanceof Number number)
                        || number.doubleValue() < 0 || number.doubleValue() % 1 != 0)) {
                    throw badRequest("followUpDays must be a non-negative integer.");
                }
            }
            case FEES -> validateFees(payload);
            case PRESCRIPTION -> validatePrescription(payload);
        }
        return payload;
    }

    private void validateFees(Map<String, Object> payload) {
        BigDecimal consultationFee = amount(payload, "consultationFee", true);
        BigDecimal procedures = amount(payload, "procedureCharges", false);
        BigDecimal facility = amount(payload, "facilityFee", false);
        BigDecimal discount = amount(payload, "discountAmount", false);
        BigDecimal tax = amount(payload, "taxAmount", false);
        BigDecimal total = consultationFee.add(procedures).add(facility).subtract(discount).add(tax);
        if (total.signum() < 0) throw badRequest("Fee total cannot be negative.");
        payload.put("consultationFee", consultationFee);
        payload.put("procedureCharges", procedures);
        payload.put("facilityFee", facility);
        payload.put("discountAmount", discount);
        payload.put("taxAmount", tax);
        payload.put("totalAmount", total.setScale(2, RoundingMode.HALF_UP));
        String paymentStatus = payload.get("paymentStatus") == null ? "PENDING"
                : String.valueOf(payload.get("paymentStatus")).toUpperCase(Locale.ROOT);
        if (!Set.of("PENDING", "PAID", "PARTIALLY_PAID", "WAIVED", "REFUNDED").contains(paymentStatus)) {
            throw badRequest("Unsupported paymentStatus.");
        }
        payload.put("paymentStatus", paymentStatus);
        optionalText(payload, "paymentChannel");
        optionalText(payload, "billingRemarks");
    }

    private void validateVitals(Map<String, Object> payload) {
        Object vitalsValue = payload.get("vitals");
        if (vitalsValue == null) return;
        Map<String, Object> vitals = asObject(vitalsValue, "vitals");
        validateRange(vitals, "bloodPressureSystolic", 30, 300);
        validateRange(vitals, "bloodPressureDiastolic", 20, 200);
        validateRange(vitals, "pulseRate", 20, 300);
        validateRange(vitals, "temperatureC", 25, 45);
        validateRange(vitals, "respiratoryRate", 4, 80);
        validateRange(vitals, "oxygenSaturationPct", 0, 100);
        validateRange(vitals, "weightKg", 0.1, 500);
        validateRange(vitals, "heightCm", 20, 300);

        Object weight = vitals.get("weightKg");
        Object height = vitals.get("heightCm");
        if (weight instanceof Number weightNumber && height instanceof Number heightNumber) {
            BigDecimal heightMeters = new BigDecimal(heightNumber.toString()).movePointLeft(2);
            BigDecimal bmi = new BigDecimal(weightNumber.toString())
                    .divide(heightMeters.multiply(heightMeters), 1, RoundingMode.HALF_UP);
            vitals.put("bmi", bmi);
        } else if (vitals.containsKey("bmi")) {
            validateRange(vitals, "bmi", 1, 150);
        }
        payload.put("vitals", vitals);
    }

    private void validateRange(Map<String, Object> object, String field, double minimum, double maximum) {
        Object value = object.get(field);
        if (value == null) return;
        if (!(value instanceof Number number)) throw badRequest(field + " must be numeric.");
        double amount = number.doubleValue();
        if (!Double.isFinite(amount) || amount < minimum || amount > maximum) {
            throw badRequest(field + " must be between " + minimum + " and " + maximum + ".");
        }
    }

    private BigDecimal amount(Map<String, Object> payload, String field, boolean required) {
        Object value = payload.get(field);
        if (value == null) {
            if (required) throw badRequest(field + " is required.");
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (!(value instanceof Number number)) throw badRequest(field + " must be a number.");
        BigDecimal amount = new BigDecimal(number.toString()).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() < 0) throw badRequest(field + " cannot be negative.");
        return amount;
    }

    private void validatePrescription(Map<String, Object> payload) {
        optionalText(payload, "prescriptionNotes");
        Object medicinesValue = payload.get("medicines");
        if (medicinesValue == null) {
            payload.put("medicines", new ArrayList<>());
            return;
        }
        if (!(medicinesValue instanceof List<?> medicines)) throw badRequest("medicines must be an array.");
        List<Object> normalized = new ArrayList<>();
        for (Object medicineValue : medicines) {
            Map<String, Object> medicine = asObject(medicineValue, "Each medicine must be a JSON object.");
            requireText(medicine, "medicineName");
            requireText(medicine, "dosage");
            requireText(medicine, "timing");
            requireText(medicine, "duration");
            optionalText(medicine, "instructions");
            normalized.add(medicine);
        }
        payload.put("medicines", normalized);
    }

    private void requireAllSectionsSaved(Consultation consultation) {
        for (ConsultationSection section : ConsultationSection.values()) {
            Map<String, Object> data = sectionData(consultation, section);
            if (data == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Save all consultation sections before completing the consultation.");
            }
            validateSection(section, data);
        }
    }

    private void requireEditableVersion(Consultation consultation, long expectedVersion) {
        if (consultation.getStatus() != ConsultationStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed consultations cannot be edited.");
        }
        if (consultation.getVersion() != expectedVersion) throw versionConflict();
    }

    private String requireText(Map<String, Object> object, String field) {
        Object value = object.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw badRequest(field + " is required and must be text.");
        }
        return text.trim();
    }

    private void optionalText(Map<String, Object> object, String field) {
        Object value = object.get(field);
        if (value != null && !(value instanceof String)) throw badRequest(field + " must be text.");
    }

    private void optionalArray(Map<String, Object> object, String field) {
        Object value = object.get(field);
        if (value != null && !(value instanceof List<?>)) throw badRequest(field + " must be an array.");
    }

    private Map<String, Object> asObject(Object value, String errorMessage) {
        if (!(value instanceof Map<?, ?> map)) throw badRequest(errorMessage);
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), deepCopyValue(item)));
        return result;
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, deepCopyValue(value)));
        return result;
    }

    private List<Object> deepCopyList(List<?> source) {
        List<Object> result = new ArrayList<>(source.size());
        source.forEach(value -> result.add(deepCopyValue(value)));
        return result;
    }

    private Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> map) return asObject(map, "JSON object expected.");
        if (value instanceof List<?> list) return deepCopyList(list);
        return value;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException versionConflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Consultation changed since it was loaded. Reload before saving again.");
    }

    private ResponseStatusException versionConflict(Throwable cause) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Consultation changed since it was loaded. Reload before saving again.", cause);
    }
}
