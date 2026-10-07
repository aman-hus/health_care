# Appointment API integration guide

This guide documents the appointment endpoints for the frontend. All paths below are relative to the backend base URL (for example, `http://localhost:8080`). Send and receive JSON except for a successful delete, which has no response body.

## Authentication

All appointment endpoints require a JWT access token. Sign in through `POST /api/auth/login` with:

```json
{
  "email": "patient@example.com",
  "password": "your-password"
}
```

The response includes the token and the signed-in user's ID and role:

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "userId": 42,
  "name": "Jordan Lee",
  "email": "patient@example.com",
  "role": "PATIENT"
}
```

Include the token on every appointment request:

```http
Authorization: Bearer <jwt>
Content-Type: application/json
```

Roles are `ADMIN`, `DOCTOR`, `PATIENT`, `NURSE`, and `MEDICAL_STAFF`. Appointment access is limited to the roles described below. The role in the login response can be used to choose the appropriate frontend views, but the backend independently checks authorization and ownership.

## Endpoint overview

| Method | Path | Allowed roles | Purpose |
|---|---|---|---|
| `POST` | `/api/appointments` | Admin, nurse, patient | Book an appointment |
| `GET` | `/api/appointments/mine` | Patient, doctor, nurse | List appointments associated with the signed-in user |
| `DELETE` | `/api/appointments/{appointmentId}` | Patient, doctor | Delete an appointment associated with the signed-in user |
| `GET` | `/api/admin/appointments` | Admin | List all appointments |
| `GET` | `/api/admin/appointments/patient/{patientId}` | Admin | List appointments for a selected patient |
| `GET` | `/api/admin/appointments/doctor/{doctorId}` | Admin | List appointments for a selected doctor |

Appointment lists are ordered by `appointmentAt`, earliest first. Empty results are returned as `200 OK` with `[]`.

## Book an appointment

`POST /api/appointments`

Request fields:

| Field | Type | Required | Notes |
|---|---|---:|---|
| `patientId` | number | Depends on role | Patient may omit it or provide their own ID. Admin and nurse must provide a patient ID. |
| `doctorId` | number | Yes | Must identify a user with role `DOCTOR`. |
| `nurseId` | number or `null` | No | If supplied, must identify a user with role `NURSE`. |
| `appointmentAt` | local date-time string | Yes | Must be in the future. Format: `YYYY-MM-DDTHH:mm:ss`. |
| `reason` | string or `null` | No | Maximum 1,000 characters. |

Example for a patient (the patient ID can be omitted because it is inferred from the token):

```json
{
  "doctorId": 7,
  "appointmentAt": "2026-10-12T10:30:00",
  "reason": "Follow-up visit"
}
```

Example for an admin or nurse booking for a patient:

```json
{
  "patientId": 42,
  "doctorId": 7,
  "nurseId": 12,
  "appointmentAt": "2026-10-12T10:30:00",
  "reason": "Follow-up visit"
}
```

Success returns `201 Created` and an appointment object (see [Appointment response](#appointment-response)). A patient cannot book for another patient. Doctors cannot book through this endpoint.

## List appointments for the signed-in user

`GET /api/appointments/mine`

No query parameters or request body. The server uses the authenticated account:

- Patient receives appointments where they are the patient.
- Doctor receives appointments assigned to them.
- Nurse receives appointments assigned to them.
- Admin and medical staff are not supported on this endpoint and receive `403 Forbidden`.

Success returns `200 OK` with an array of appointment objects. For a patient or doctor, this is the endpoint to load their appointment list.

## Delete an appointment

`DELETE /api/appointments/{appointmentId}`

No request body. A patient can delete an appointment only when they are its patient. A doctor can delete an appointment only when it is assigned to them. Admins, nurses, and medical staff cannot delete appointments using this endpoint.

Success returns `200 OK` with a confirmation message. The frontend can display the message, then remove the item from its current list or reload the list. Attempting to delete another user's appointment returns `403 Forbidden`; a nonexistent appointment returns `404 Not Found`.

```json
{
  "message": "Appointment deleted successfully."
}
```

## Admin appointment lists

These endpoints require an admin JWT.

### All appointments

`GET /api/admin/appointments`

Returns all appointments ordered by appointment time. Use this for the admin's complete appointments view.

### Appointments for one patient

`GET /api/admin/appointments/patient/{patientId}`

Replace `{patientId}` with the selected patient's user ID. The ID must belong to a user with role `PATIENT`. A missing user returns `404 Not Found`; an existing user with another role returns `400 Bad Request`.

### Appointments for one doctor

`GET /api/admin/appointments/doctor/{doctorId}`

Replace `{doctorId}` with the selected doctor's user ID. The ID must belong to a user with role `DOCTOR`. A missing user returns `404 Not Found`; an existing user with another role returns `400 Bad Request`.

Both scoped endpoints return the same appointment response shape as the all-appointments endpoint. They may return an empty array if the selected user has no appointments.

## Appointment response

Each appointment in a successful response has this shape:

```json
{
  "id": 101,
  "patientId": 42,
  "patientName": "Jordan Lee",
  "doctorId": 7,
  "doctorName": "Dr. Sam Patel",
  "nurseId": 12,
  "nurseName": "Taylor Kim",
  "appointmentAt": "2026-10-12T10:30:00",
  "reason": "Follow-up visit",
  "createdAt": "2026-10-06T14:22:31.412"
}
```

`nurseId` and `nurseName` are `null` when no nurse is assigned. `appointmentAt` and `createdAt` are ISO local date-time values without a timezone offset; the API currently does not attach a timezone to these values. `id` is the appointment ID used in the delete path.

List response example:

```json
[
  {
    "id": 101,
    "patientId": 42,
    "patientName": "Jordan Lee",
    "doctorId": 7,
    "doctorName": "Dr. Sam Patel",
    "nurseId": null,
    "nurseName": null,
    "appointmentAt": "2026-10-12T10:30:00",
    "reason": "Follow-up visit",
    "createdAt": "2026-10-06T14:22:31.412"
  }
]
```

## Errors

Application errors generally use this JSON shape:

```json
{
  "timestamp": "2026-10-06T14:22:31.412",
  "status": 403,
  "error": "Forbidden",
  "message": "You can only delete appointments associated with your account."
}
```

| Status | Meaning in appointment integration |
|---:|---|
| `201` | Appointment created |
| `200` | Appointment list returned |
| `200` | Appointment deleted; response contains a confirmation message |
| `400` | Invalid fields, past appointment time, or selected ID has the wrong role |
| `401` | Missing or invalid authentication token |
| `403` | Role is not permitted, or the caller does not own the appointment |
| `404` | Appointment or selected patient/doctor does not exist |

## Frontend integration flows

### Patient

1. Sign in and retain the JWT and `userId` for the session.
2. Load appointments with `GET /api/appointments/mine`.
3. Book with `POST /api/appointments`; omit `patientId` or send the signed-in `userId`.
4. Delete an appointment with `DELETE /api/appointments/{id}` after the patient confirms the action.
5. Show the confirmation message and refresh the list or remove the deleted row after `200`.

### Doctor

1. Sign in and load assigned appointments with `GET /api/appointments/mine`.
2. Delete an appointment assigned to them with `DELETE /api/appointments/{id}`.

### Admin

1. Load all appointments with `GET /api/admin/appointments` when needed.
2. When an admin selects a patient, request `GET /api/admin/appointments/patient/{patientId}`.
3. When an admin selects a doctor, request `GET /api/admin/appointments/doctor/{doctorId}`.

## cURL examples

```sh
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/appointments/mine
```

```sh
curl -X POST http://localhost:8080/api/appointments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"doctorId":7,"appointmentAt":"2026-10-12T10:30:00","reason":"Follow-up visit"}'
```

```sh
curl -X DELETE http://localhost:8080/api/appointments/101 \
  -H "Authorization: Bearer $TOKEN"
```

```sh
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/admin/appointments/patient/42
```

```sh
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/admin/appointments/doctor/7
```
