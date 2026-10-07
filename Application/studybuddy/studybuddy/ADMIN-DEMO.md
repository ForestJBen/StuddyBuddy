# Local administrator preview

Run from this directory:

```sh
sh mvnw spring-boot:test-run -Dspring-boot.run.main-class=studdybuddy.AdminDemoApplication
```

Open http://127.0.0.1:18081/login?admin=true

Email: `admin@gmail.com`
Password: `admin123456789`

Requires Java 21 or newer. No MySQL server or manual SQL is needed. The launcher uses a disposable H2 database; data is lost when it stops. Stop with Ctrl+C. Port 18081 must be free.

The account seeder and launcher live in test sources and are excluded from production JARs. The demo binds to loopback only and refuses to seed a non-memory database. Normal startup does not create this account.

The dashboard supports course creation/editing, restricted deletion, member addition/removal/moves, and user disable/restore. All writes require administrator access and CSRF protection. Successful changes and their audit records commit together.

Sample users and two study groups are created automatically. Use Alex Member to try moving between groups and Casey Unlinked to try disabling/deleting an unused account. Sample student password: `demo123456789` (local demo only).

Rules:
- All administrator accounts are protected from disabling/deletion.
- A user must be disabled and have no linked records before permanent deletion.
- Group creators cannot be removed/moved; ownership transfer is not implemented.
- Moving a user does not move historical posts. Already belonging to the destination is a conflict.
- Courses referenced by records cannot be deleted. A course code used by a group cannot be renamed.
- Existing sessions are checked on each request and rejected for disabled/deleted accounts.
- Activity shows the most recent 50 successful writes; all audit rows remain stored.

Production deployments use Flyway migration V7 to add account status and the audit table. H2 tests do not validate the MySQL migration; run the deployment CI against MySQL before release.
