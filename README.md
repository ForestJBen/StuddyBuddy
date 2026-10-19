# StudyBuddy

StudyBuddy is a student study partner application. Students can create an account, add courses and study preferences, find suggested partners, exchange messages and organise study groups. It also includes AI translation, study session planning and session summaries.

Repository: [ForestJBen/StuddyBuddy](https://github.com/ForestJBen/StuddyBuddy)

## Technology and project structure

The application uses Java 21, Spring Boot, Spring Security, Spring Data JPA and Thymeleaf. HTML, CSS and JavaScript provide the browser interface. MySQL stores application data, and Flyway manages database schema changes. H2 is available for temporary previews and tests. AI features call the OpenAI API.

The frontend is served by the Spring Boot application; there is no separate Node.js or npm setup.

```text
StuddyBuddy/
├── Application/studybuddy/studybuddy/
│   ├── pom.xml                    # Maven dependencies and build configuration
│   ├── mvnw / mvnw.cmd            # Maven Wrapper for macOS/Linux and Windows
│   └── src/
│       ├── main/
│       │   ├── java/studdybuddy/  # Controllers, services, entities and repositories
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── db/migration/  # Flyway SQL migrations
│       │       ├── templates/     # Thymeleaf page templates
│       │       └── static/        # CSS and JavaScript
│       └── test/                  # Automated tests and H2 configuration
├── Database/
│   ├── ERD/                       # Database diagram
│   └── schema/                    # Earlier database schema reference
├── Documents/                     # API contracts and integration notes
└── tests/                         # Additional browser test scripts
```

## 1. Install the prerequisites

- Install **JDK 21** and ensure `java` is available in your terminal. The project targets Java 21; a compatible newer JDK can also be used.
- Install **Git**, or download the repository as a ZIP file.
- For the normal persistent setup, install and start **MySQL 8.x** and have access to its command-line client or MySQL Workbench.
- Use a modern browser such as Chrome, Firefox or Edge.
- Internet access is needed for the first Maven dependency download. The AI features also need an API key and internet access.

Check Java before continuing:

```text
java -version
```

Maven is downloaded automatically by the included wrapper. You do not need to install Maven separately. If you only want a temporary preview without MySQL, follow steps 1–2 and then use the **Temporary preview** section below.

## 2. Download the project

```sh
git clone https://github.com/ForestJBen/StuddyBuddy.git
cd StuddyBuddy/Application/studybuddy/studybuddy
```

If the repository is private, sign in with a GitHub account that has access. Alternatively, use GitHub's **Code → Download ZIP**, extract it, and open a terminal in the extracted `Application/studybuddy/studybuddy` directory. All Maven commands below run from this directory, which contains `pom.xml`.

## 3. Prepare the MySQL database

Start your MySQL server. Connect using a database administrator account, for example:

```sh
mysql -u root -p
```

Run the following SQL in the MySQL client or a MySQL Workbench query tab. Replace the example password with your own password and use the same value in step 4.

```sql
CREATE DATABASE studybuddy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'studybuddy_app'@'localhost' IDENTIFIED BY 'replace_with_your_password';
GRANT ALL PRIVILEGES ON studybuddy.* TO 'studybuddy_app'@'localhost';
```

Then leave the command-line client with `exit`.

Use a fresh, empty database for the first run. **Do not manually import `Database/schema/first_database.sql`.** Flyway runs the versioned scripts in `src/main/resources/db/migration/` when the application starts. Hibernate then validates the resulting schema. The sample course list is populated automatically at startup.

## 4. Configure the database connection

Set these environment variables in the same terminal where you will start the application.

**macOS / Linux:**

```sh
export DB_URL='jdbc:mysql://localhost:3306/studybuddy'
export DB_USERNAME='studybuddy_app'
export DB_PASSWORD='replace_with_your_password'
```

**Windows PowerShell:**

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/studybuddy'
$env:DB_USERNAME = 'studybuddy_app'
$env:DB_PASSWORD = 'replace_with_your_password'
```

Change the hostname, port or database name in `DB_URL` if your MySQL installation uses different values. These variables are read by `src/main/resources/application.properties`. A `.env` file is not loaded automatically. If starting from an IDE, set the variables in its run configuration.

### Optional: enable AI features

To use translation, session plans and summaries, set your own OpenAI API key before starting the application:

```sh
# macOS / Linux
export OPENAI_API_KEY='your_api_key'
```

```powershell
# Windows PowerShell
$env:OPENAI_API_KEY = 'your_api_key'
```

The key must have access to the model configured in `src/main/java/studdybuddy/service/AiService.java`. API usage may incur charges. Keep the key out of source files and Git commits. Without a key, the application can start and the non-AI features remain available, but AI requests report that the service is unavailable.

## 5. Start the application

**macOS / Linux:**

```sh
sh mvnw spring-boot:run
```

**Windows PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

The first run downloads the required dependencies. Keep the terminal open and wait for the log to report that the application has started. Open:

**[http://localhost:8080/](http://localhost:8080/)**

Stop the application with **Ctrl+C**. With MySQL, saved data remains available after restarting.

If port 8080 is already in use, start on another port:

```sh
# macOS / Linux
sh mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

```powershell
# Windows PowerShell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=8081'
```

Then open `http://localhost:8081/`.

## 6. Create an account and use StudyBuddy

1. Select **Create account** and enter your first name, last name, email address, password and password confirmation. Passwords must contain 8–72 characters.
2. Select **Log in** and sign in with the registered email address and password.
3. Open **Profile**, add your programme, year, courses, study goals, preferred study style and availability, then select **Save profile draft**.
4. Open **Find Buddies** to view matching results. For a new database, create another student account and save its study profile before expecting recommendations. Use **Message** on a result card to open a conversation.
5. Open **Groups** to create a group or join an existing one. Group creators can edit their group details or open the deletion confirmation dialog.
6. Open **Sessions** to access the study session tools. AI generation requires the optional API key configuration above.

Newly registered accounts have the student role. Registration does not create an administrator. An existing user can be given the admin role by the database administrator, using the registered email address:

```sql
UPDATE studybuddy.users SET role = 'ADMIN' WHERE email = 'your_registered_email@example.com';
```

Log out and sign in again after this change. The administrator interface is at `/admin`.

## Temporary preview without MySQL

For a disposable local preview, use the H2 configuration in `src/test/resources/application.properties`:

```sh
# macOS / Linux
sh mvnw spring-boot:test-run
```

```powershell
# Windows PowerShell
.\mvnw.cmd spring-boot:test-run
```

Open `http://localhost:8080/` and follow the account creation steps above. This command includes test resources and dependencies, uses an in-memory H2 database, disables Flyway and creates tables from the entity mappings. **All preview data is lost when the application stops.** This preview does not verify the MySQL migration scripts and should not be used for persistent data.

To use a different port, append `-Dspring-boot.run.arguments=--server.port=8081` (quote the whole argument in PowerShell).

## Automated tests

Run these commands from the directory containing `pom.xml`. Most tests use the disposable H2 database. To run these without MySQL, exclude the MySQL-specific group integration tests:

**macOS / Linux:**

```sh
sh mvnw test '-Dtest=!GroupControllerIntegrationTests'
```

**Windows PowerShell:**

```powershell
.\mvnw.cmd test '-Dtest=!GroupControllerIntegrationTests'
```

Reports are written to `target/surefire-reports/`. These H2 tests do not validate MySQL migrations or live AI responses.

`GroupControllerIntegrationTests` requires MySQL and deletes group, message-board and user records as part of its setup and cleanup. Run it only against a separate disposable database. Create an empty database named `studybuddy_group_test`, give the database user permission to initialise it, and set `DB_URL` to `jdbc:mysql://localhost:3306/studybuddy_group_test` using the environment-variable syntax in step 4. Then run `sh mvnw test` (macOS/Linux) or `.\mvnw.cmd test` (Windows). Never point this test at a database containing data you need.

## Troubleshooting

| Problem | What to check |
| --- | --- |
| Java version or compiler error | Confirm the terminal and IDE use a compatible JDK, with Java 21 as the project target. |
| `mvnw` cannot be executed on macOS/Linux | Use `sh mvnw ...` as shown above; the script does not need executable permission. |
| Database connection refused / communications link failure | Check that MySQL is running, the configured host and port are reachable, and the database exists. |
| Access denied for the database user | Check `DB_USERNAME`, `DB_PASSWORD` and the user's permissions on `studybuddy`. |
| Flyway reports existing tables or a schema conflict | Use a separate empty database and let Flyway initialise it. Do not delete an existing database containing work you need. |
| Port already in use | Stop your other application or choose a different server port as shown in step 5. |
| No matching students | Create and save another student profile with overlapping courses and study preferences. |
| AI service unavailable | Check that `OPENAI_API_KEY` was set before launch and has access and available usage for the configured model. |
| H2 preview data disappeared | The preview database is temporary. Use the MySQL setup for persistent data. |
