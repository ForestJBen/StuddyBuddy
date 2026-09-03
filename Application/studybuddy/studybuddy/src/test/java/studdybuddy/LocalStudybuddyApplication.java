package studdybuddy;

import org.springframework.boot.SpringApplication;

/**
 * Local-only launcher that includes test dependencies such as the in-memory H2 database.
 * Run this class from the IDE until the shared MySQL configuration is available.
 */
public final class LocalStudybuddyApplication {

    private LocalStudybuddyApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.from(StudybuddyApplication::main).run(args);
    }
}
