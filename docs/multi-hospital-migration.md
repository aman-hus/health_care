# Multi-hospital rollout

The API now treats each hospital as a tenant. An account can have a different role in each hospital through `hospital_memberships`; the hospital selected at login is stored in the signed JWT and checked against an active membership on every API request.

## Existing installation

1. Back up the PostgreSQL database.
2. Run `src/main/resources/db/migrate-to-multi-hospital.sql` once against the existing database. It creates the `DEFAULT` hospital, assigns existing users to it, marks the earliest existing `ADMIN` as the initial platform admin, and associates existing appointments with it.
3. Deploy the updated application. Keep `spring.jpa.hibernate.ddl-auto=update` during rollout so Hibernate can apply the entity mappings after the data migration. For production, use controlled SQL migrations rather than relying on automatic schema updates.
4. Existing JWTs do not contain a hospital ID; users must sign in again to receive a tenant-scoped token.

Do not run the existing-data migration on a brand-new empty database. On a new installation, use `/api/auth/bootstrap-admin` to create only the first platform admin, then log in and create hospitals separately.

## Setup and day-to-day APIs

- `GET /api/auth/hospitals` lists active hospitals for the sign-in selector.
- `POST /api/auth/bootstrap-admin` accepts only the first administrator's `name`, `email`, and `password`. It is available only until the first user has been created.
- The first platform admin can log in with `POST /api/auth/login` without `hospitalId`; the resulting JWT has no hospital context and is used for platform-level hospital management.
- A platform admin creates a hospital and its first hospital-admin account with `POST /api/admin/hospitals`, passing `name`, `code`, `adminName`, `adminEmail`, and `adminPassword`. The platform admin is not added as a member of the hospital.
- Hospital admins log in with the new hospital's ID. They then add users to their selected hospital with `POST /api/admin/users` and the role for that hospital. Existing accounts may be added to another hospital; new accounts are created as part of that operation.
- Tenant-scoped login responses contain `hospitalId`, the membership role, and a JWT bound to that hospital. Send the JWT as `Authorization: Bearer <token>` for API calls; do not choose tenant context with an untrusted request header.
- Appointment and user-directory operations are filtered to the hospital in the access token. Appointment booking also checks that the patient, doctor, and optional nurse are members of that same hospital.

## Current scope and follow-up

Patient login accounts and `PatientProfile` remain shared across hospitals; profiles currently contain demographics and emergency contact information. There is no medical-record entity yet. If patient identity or clinical records must be isolated per hospital, add a hospital-specific patient record/encounter model before storing those records. The existing `AppUser.role` remains for password authentication and compatibility; authorization uses the role in `HospitalMembership`.
