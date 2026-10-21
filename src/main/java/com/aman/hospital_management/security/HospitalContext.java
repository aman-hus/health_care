package com.aman.hospital_management.security;

/** Request-scoped hospital selected by a signed access token. */
public final class HospitalContext {
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();
    private HospitalContext() { }
    public static void set(Long hospitalId) { CURRENT.set(hospitalId); }
    public static Long requireHospitalId() {
        Long id = CURRENT.get();
        if (id == null) throw new IllegalStateException("Authenticated request has no hospital context.");
        return id;
    }
    public static void clear() { CURRENT.remove(); }
}
