package com.aman.hospital_management.model;

public enum ConsultationSection {
    DIAGNOSIS,
    EVALUATION,
    CARE_PLAN,
    FEES,
    PRESCRIPTION;

    public ConsultationSection previous() {
        return switch (this) {
            case DIAGNOSIS -> null;
            case EVALUATION -> DIAGNOSIS;
            case CARE_PLAN -> EVALUATION;
            case FEES -> CARE_PLAN;
            case PRESCRIPTION -> FEES;
        };
    }

    public ConsultationSection next() {
        return switch (this) {
            case DIAGNOSIS -> EVALUATION;
            case EVALUATION -> CARE_PLAN;
            case CARE_PLAN -> FEES;
            case FEES, PRESCRIPTION -> PRESCRIPTION;
        };
    }
}
