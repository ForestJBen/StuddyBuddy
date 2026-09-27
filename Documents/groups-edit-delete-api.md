# Groups edit and delete API handoff

The frontend now displays Edit group and Delete group for the creator. The dialogs call the following proposed endpoints. Backend implementation is still required; the frontend must not be considered end-to-end complete until these endpoints are implemented and tested.

## Rules

- Only the authenticated group creator can edit or delete. Enforce this in the backend; hiding buttons is not authorization.
- Editing updates name, course, description, meetingTime and location only. Keep id, creator, createdAt, memberships and posts unchanged.
- A creator may delete a group even if it has other members or posts. Deletion permanently removes all group posts and memberships, then the group, in one transaction. Never delete user accounts or one-to-one messages.
- The UI requires a confirmation checkbox and explains that deletion cannot be undone.
- If other tables reference a group (for example study_sessions or partner_requests), coordinate their handling with Forest. Do not silently delete unrelated data. Until those references are handled safely, reject with 409 and a clear message, rolling back the entire deletion.
- All writes use the existing session authentication and CSRF protection. Never take creatorId or userId from the body.

## Edit

`PUT /api/groups/{groupId}`

Content-Type: application/json

```json
{
  "name": "159.302 Revision Group",
  "course": "159.302",
  "description": "Prepare for the final exam",
  "meetingTime": "Saturday 2:00 PM",
  "location": "Library Level 2"
}
```

Full replacement of these five editable fields. Empty strings clear optional meetingTime/location. Validation follows CreateGroupRequest: trimmed nonblank name <=120, course <=20, description <=2000; optional meetingTime <=100, location <=200 characters.

Success: `200 OK` with the full existing GroupResponse schema:

```json
{
  "id": 1,
  "name": "159.302 Revision Group",
  "course": "159.302",
  "description": "Prepare for the final exam",
  "meetingTime": "Saturday 2:00 PM",
  "location": "Library Level 2",
  "creatorId": 11,
  "creatorName": "Alex Chen",
  "createdAt": "2026-09-27T10:00:00Z",
  "memberCount": 2,
  "joined": true
}
```

The response reflects the authenticated creator. The frontend updates the card and open details and refreshes the list only after a successful response.

## Delete

`DELETE /api/groups/{groupId}`

No request body. Success: `204 No Content` with an empty response body. After success, subsequent list calls omit the group; its member/post endpoints return 404. The UI closes any open detail for the deleted group and refreshes the list.

## Errors

Use existing ApiErrorResponse JSON, for example:

```json
{"success": false, "message": "Only the group creator can edit this group"}
```

- 400: validation failure, optional fieldErrors with request field names.
- 401: login required.
- 403: authenticated user is not the creator, or CSRF validation failed.
- 404: group does not exist / has already been deleted.
- 409: deletion blocked by other linked records or another business conflict; provide a clear message.
- 500: unexpected failure, no partial deletion.

Do not return 200 with success:false for failures. The frontend preserves dialog input on failure and does not remove the group or show a success message.

## Backend acceptance tests

1. Creator can edit all five fields; refresh retains the values. Members/posts, creator and creation time remain unchanged.
2. Ordinary members and nonmembers cannot edit/delete, including direct API calls.
3. Creator can delete a populated group; its memberships and group posts are deleted atomically. User accounts and private messages remain.
4. Missing group, invalid input, missing login and invalid CSRF return the expected status.
5. A failed deletion leaves the group, memberships and posts intact. Verify actual MySQL foreign keys as well as H2.

Frontend files: templates/groups.html, static/js/groups.js, static/css/groups.css.
