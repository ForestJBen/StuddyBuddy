package studdybuddy;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import studdybuddy.user.*;

/** Explicitly imported by the demo launcher only; excluded from production artifacts. */
@TestConfiguration(proxyBeanMethods = false)
public class AdminDemoConfiguration {
    @Bean
    ApplicationRunner seedDemoAdmin(UserRepository users, PasswordEncoder passwords, Environment environment,
            studdybuddy.group.StudyGroupRepository groups, studdybuddy.group.GroupMembershipRepository members) {
        return args -> {
            if (!environment.getRequiredProperty("spring.datasource.url").startsWith("jdbc:h2:mem:")) {
                throw new IllegalStateException("Admin demo requires an in-memory H2 database");
            }
            if (users.findByEmail("admin@gmail.com").isPresent()) {
                throw new IllegalStateException("Demo admin email already exists; refusing to overwrite it");
            }
            users.save(new User("Demo", "Admin", "admin@gmail.com",
                    passwords.encode("admin123456789"), UserRole.ADMIN));
            var owner=users.save(new User("Taylor", "Owner", "owner@example.test", passwords.encode("demo123456789"), UserRole.STUDENT));
            var member=users.save(new User("Alex", "Member", "member@example.test", passwords.encode("demo123456789"), UserRole.STUDENT));
            users.save(new User("Casey", "Unlinked", "unused@example.test", passwords.encode("demo123456789"), UserRole.STUDENT));
            var first=groups.save(new studdybuddy.group.StudyGroup("Programming practice", "159.101", "Demo study group", null, null, owner));
            var second=groups.save(new studdybuddy.group.StudyGroup("Database study", "159.221", "Demo destination group", null, null, owner));
            members.save(new studdybuddy.group.GroupMembership(first,owner));
            members.save(new studdybuddy.group.GroupMembership(second,owner));
            members.save(new studdybuddy.group.GroupMembership(first,member));
        };
    }
}
