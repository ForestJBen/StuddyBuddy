# Profile Frontend Integration Guide

## Purpose

This page lets a student edit the profile information used for future matching. It is intentionally separated into one HTML file, one CSS file and one JavaScript file so frontend and backend work can be reviewed independently.

## Files

- `src/main/resources/templates/profile.html` - semantic page structure and form fields
- `src/main/resources/static/css/profile.css` - Profile page styles
- `src/main/resources/static/js/profile.js` - form state, validation, API requests and local draft fallback
- `HomeController.java` - maps `GET /profile` to the Thymeleaf template

All application paths above are inside `Application/studybuddy/studybuddy`.

## Current development mode

In `profile.js`, this setting is currently disabled:

```javascript
profileApiEnabled: false
```

While disabled:

- The page tries `GET /api/courses` first.
- If the Course API is unavailable, temporary course objects are displayed.
- Saving writes a draft to browser `localStorage` only.
- The interface clearly says that backend saving is not connected.

## Backend contract

The frontend expects:

```http
GET /api/courses
GET /api/profile
PUT /api/profile
```

The Profile update request is:

```json
{
  "programme": "Bachelor of Information Sciences",
  "year": 2,
  "courseIds": [1, 3],
  "goals": "Prepare for exams and improve Java skills",
  "studyStyle": "Group discussion",
  "availability": "Weekday evenings"
}
```

The Profile response must return complete course objects:

```json
{
  "id": 15,
  "programme": "Bachelor of Information Sciences",
  "year": 2,
  "courses": [
    {
      "id": 1,
      "courseCode": "COMP101",
      "courseName": "Introduction to Programming"
    }
  ],
  "goals": "Prepare for exams and improve Java skills",
  "studyStyle": "Group discussion",
  "availability": "Weekday evenings"
}
```

## Connecting Jacob's Profile API

After the API is merged and its response matches this contract:

1. Open `src/main/resources/static/js/profile.js`.
2. Change `profileApiEnabled` from `false` to `true`.
3. Run the application and open `http://localhost:8080/profile`.
4. Confirm the page loads the authenticated user's profile.
5. Edit the form and confirm `PUT /api/profile` persists the changes.
6. Check validation errors and CSRF behavior before removing the local fallback.

The JavaScript already sends same-origin credentials and automatically includes a Thymeleaf CSRF token when the token meta tags are available.

## IntelliJ IDEA run instructions

1. Open the repository root folder `StuddyBuddy` in IntelliJ IDEA.
2. Select JDK 21 for the nested Maven application.
3. Open `StudybuddyApplication.java`.
4. Run `StudybuddyApplication` using the green Run button.
5. Open `http://localhost:8080/profile` in a browser.

Until database configuration is ready, keep the existing local IntelliJ run options that exclude the database auto-configuration.
