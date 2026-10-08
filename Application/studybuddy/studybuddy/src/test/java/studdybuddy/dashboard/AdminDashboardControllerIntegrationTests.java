package studdybuddy.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;
import studdybuddy.user.UserRole;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void unauthenticatedUserCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotAccessDashboard() throws Exception {
        saveUser("student@test.com", UserRole.STUDENT);

        mockMvc.perform(get("/api/admin/dashboard")
                        .with(user("student@test.com").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessDashboardAndSeeCorrectCounts() throws Exception {
        saveUser("admin@test.com", UserRole.ADMIN);
        saveUser("student@test.com", UserRole.STUDENT);

        mockMvc.perform(get("/api/admin/dashboard")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.studentUsers").value(1))
                .andExpect(jsonPath("$.adminUsers").value(1));
    }

    @Test
    void adminCanViewUsersWithoutPasswordHash() throws Exception {
        saveUser("admin@test.com", UserRole.ADMIN);
        saveUser("student@test.com", UserRole.STUDENT);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "20")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].email").exists())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    private void saveUser(String email, UserRole role) {
        User user = new User(
                "Test",
                "User",
                email,
                passwordEncoder.encode("Password123!"),
                role
        );

        userRepository.save(user);
    }
}