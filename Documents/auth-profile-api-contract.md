# StudyBuddy Authentication and Profile API Contract

## Status

Proposed MVP contract for frontend and backend coordination. The frontend and backend owners should review and agree on this contract before implementation is considered final.

## Scope

This document covers:

- Student registration
- Login and logout
- Current authenticated user
- Student profile retrieval and update
- Shared success and error response conventions

Password reset, social login and advanced skills portfolios are outside the current MVP scope.

## Authentication approach

Use Spring Security session and cookie authentication for the MVP. The frontend and backend are served by the same Spring Boot application, so JWT authentication is not currently required.

Public registration must always create a `STUDENT` account. The backend must not accept an `ADMIN` role from the public registration request.

Passwords must never be stored as plain text. Only a secure password hash produced by Spring Security should be stored.

## General conventions

- Base path: `/api`
- Request and response format: `application/json`
- Time format: 24-hour `HH:mm`
- Enum values use uppercase strings, for example `STUDENT` and `COLLABORATIVE`
- Validation errors use the shared error structure defined below
- Authentication cookies should use the appropriate `HttpOnly`, `Secure` and `SameSite` settings for the deployment environment

## Shared error response

All endpoints should use a consistent error structure:

```json
{
  "success": false,
  "message": "Please correct the highlighted fields",
  "fieldErrors": {
    "email": "Please enter a valid email address"
  }
}
```

`fieldErrors` may be omitted when the error is not associated with a specific field.

## Register student

### Request

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "firstName": "Alex",
  "lastName": "Chen",
  "email": "alex@example.com",
  "password": "Password123!",
  "confirmPassword": "Password123!"
}
```

`confirmPassword` is used for validation and must not be stored.

### Success

```http
201 Created
```

```json
{
  "success": true,
  "message": "Account created successfully",
  "user": {
    "id": 12,
    "firstName": "Alex",
    "lastName": "Chen",
    "email": "alex@example.com",
    "role": "STUDENT"
  }
}
```

### Validation failure

```http
400 Bad Request
```

```json
{
  "success": false,
  "message": "Please correct the highlighted fields",
  "fieldErrors": {
    "email": "Please enter a valid email address",
    "password": "Password must contain at least 8 characters"
  }
}
```

### Email already registered

```http
409 Conflict
```

```json
{
  "success": false,
  "message": "An account with this email already exists"
}
```

## Login

### Request

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "alex@example.com",
  "password": "Password123!"
}
```

### Success

```http
200 OK
```

```json
{
  "success": true,
  "message": "Login successful",
  "user": {
    "id": 12,
    "firstName": "Alex",
    "lastName": "Chen",
    "role": "STUDENT"
  },
  "redirectUrl": "/dashboard"
}
```

### Invalid credentials

```http
401 Unauthorized
```

```json
{
  "success": false,
  "message": "Invalid email or password"
}
```

The response must not reveal whether the email address or password was incorrect.

## Logout

### Request

```http
POST /api/auth/logout
```

### Success

```http
204 No Content
```

The backend should invalidate the current session and clear the authentication cookie as appropriate.

## Get current user

### Request

```http
GET /api/auth/me
```

### Success

```http
200 OK
```

```json
{
  "id": 12,
  "firstName": "Alex",
  "lastName": "Chen",
  "email": "alex@example.com",
  "role": "STUDENT"
}
```

### Not authenticated

```http
401 Unauthorized
```

## Get student profile

### Request

```http
GET /api/profile
```

### Success

```http
200 OK
```

```json
{
  "id": 12,
  "firstName": "Alex",
  "lastName": "Chen",
  "email": "alex@example.com",
  "programme": "Bachelor of Information Sciences",
  "year": 2,
  "courses": ["COMP101", "COMP161"],
  "goals": ["Exam preparation", "Weekly revision"],
  "studyStyle": "COLLABORATIVE",
  "availability": [
    {
      "day": "SATURDAY",
      "startTime": "13:00",
      "endTime": "16:00"
    }
  ]
}
```

## Update student profile

### Request

```http
PUT /api/profile
Content-Type: application/json
```

```json
{
  "programme": "Bachelor of Information Sciences",
  "year": 2,
  "courses": ["COMP101", "COMP161"],
  "goals": ["Exam preparation"],
  "studyStyle": "COLLABORATIVE",
  "availability": [
    {
      "day": "SATURDAY",
      "startTime": "13:00",
      "endTime": "16:00"
    }
  ]
}
```

### Success

```http
200 OK
```

The response should contain the complete updated profile using the same structure as `GET /api/profile`.

### Validation failure

```http
400 Bad Request
```

Use the shared error response with field-specific messages where possible.

## Suggested minimum database fields

### users

- `id`
- `first_name`
- `last_name`
- `email`
- `password_hash`
- `role`
- `created_at`
- `updated_at`

### profiles

- `id`
- `user_id`
- `programme`
- `year`
- `study_style`

Courses, study goals and availability should use related tables rather than comma-separated database fields. Suggested tables include:

- `courses`
- `user_courses`
- `study_goals`
- `user_goals`
- `availability`

## Ownership and review questions

Frontend and backend owners should confirm:

1. Whether the endpoint paths fit the backend structure
2. Whether session-based authentication is accepted
3. The final registration and profile fields
4. The exact validation rules
5. Whether CSRF protection requires a token in frontend requests
6. Whether successful registration logs the student in automatically or redirects to login
7. Whether profile updates use `PUT` for complete replacement or `PATCH` for partial updates

After agreement, changes to this contract should be reviewed by both frontend and backend owners before implementation changes are merged.
