package studdybuddy.notification;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;
import studdybuddy.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUpDatabase() {
        clearDatabase();
    }

    @Test
    void unauthenticatedGetAndPatchReturnJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication is required"));

        mockMvc.perform(patch("/api/notifications/1/read").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void getReturnsOnlyCurrentUsersNotificationsNewestFirst() throws Exception {
        MockHttpSession alexSession = createUserAndLogin("alex@example.com");
        createUserAndLogin("sam@example.com");

        User alex = userRepository.findByEmail("alex@example.com").orElseThrow();
        User sam = userRepository.findByEmail("sam@example.com").orElseThrow();

        Notification oldNotification = notificationRepository.save(
                new Notification(alex, "Old notification")
        );
        Notification newNotification = notificationRepository.save(
                new Notification(alex, "New notification")
        );
        notificationRepository.save(new Notification(sam, "Sam's notification"));

        setCreatedAt(oldNotification, "2026-09-01T08:00:00Z");
        setCreatedAt(newNotification, "2026-09-02T08:00:00Z");

        mockMvc.perform(get("/api/notifications").session(alexSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].message").value("New notification"))
                .andExpect(jsonPath("$[1].message").value("Old notification"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void patchMarksCurrentUsersNotificationAsRead() throws Exception {
        MockHttpSession session = createUserAndLogin("alex@example.com");
        User alex = userRepository.findByEmail("alex@example.com").orElseThrow();
        Notification notification = notificationRepository.save(
                new Notification(alex, "Your partner request was accepted")
        );

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId()))
                .andExpect(jsonPath("$.read").value(true));

        Notification savedNotification = notificationRepository.findById(notification.getId())
                .orElseThrow();
        assertThat(savedNotification.isRead()).isTrue();
    }

    @Test
    void patchingAnotherUsersNotificationReturnsNotFoundAndChangesNothing() throws Exception {
        createUserAndLogin("alex@example.com");
        MockHttpSession samSession = createUserAndLogin("sam@example.com");

        User alex = userRepository.findByEmail("alex@example.com").orElseThrow();
        Notification alexNotification = notificationRepository.save(
                new Notification(alex, "Only Alex can read this")
        );

        mockMvc.perform(patch("/api/notifications/{id}/read", alexNotification.getId())
                        .with(csrf())
                        .session(samSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Notification not found"));

        Notification unchangedNotification = notificationRepository
                .findById(alexNotification.getId())
                .orElseThrow();
        assertThat(unchangedNotification.isRead()).isFalse();
    }

    private MockHttpSession createUserAndLogin(String email) throws Exception {
        userRepository.save(new User(
                "Alex",
                "Chen",
                email,
                passwordEncoder.encode("Password123!"),
                UserRole.STUDENT
        ));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private void setCreatedAt(Notification notification, String time) {
        jdbcTemplate.update(
                "UPDATE notifications SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse(time)),
                notification.getId()
        );
    }
}