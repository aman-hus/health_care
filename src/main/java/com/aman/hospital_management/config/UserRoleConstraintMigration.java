package com.aman.hospital_management.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keeps the PostgreSQL role check in sync with the Role enum. */
@Component
public class UserRoleConstraintMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public UserRoleConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM pg_constraint c
                        JOIN pg_class t ON t.oid = c.conrelid
                        JOIN pg_namespace n ON n.oid = t.relnamespace
                        WHERE c.conname = 'users_role_check'
                          AND t.relname = 'users'
                          AND n.nspname = current_schema()
                    ) THEN
                        IF NOT EXISTS (
                            SELECT 1
                            FROM pg_constraint c
                            JOIN pg_class t ON t.oid = c.conrelid
                            JOIN pg_namespace n ON n.oid = t.relnamespace
                            WHERE c.conname = 'users_role_check'
                              AND t.relname = 'users'
                              AND n.nspname = current_schema()
                              AND pg_get_constraintdef(c.oid) LIKE '%PLATFORM_ADMIN%'
                        ) THEN
                            ALTER TABLE users DROP CONSTRAINT users_role_check;
                            ALTER TABLE users
                                ADD CONSTRAINT users_role_check
                                CHECK (role IN (
                                    'PLATFORM_ADMIN', 'ADMIN', 'DOCTOR',
                                    'PATIENT', 'NURSE', 'MEDICAL_STAFF'
                                ));
                        END IF;
                    ELSE
                        ALTER TABLE users
                            ADD CONSTRAINT users_role_check
                            CHECK (role IN (
                                'PLATFORM_ADMIN', 'ADMIN', 'DOCTOR',
                                'PATIENT', 'NURSE', 'MEDICAL_STAFF'
                            ));
                    END IF;
                END $$;
                """);
    }
}
