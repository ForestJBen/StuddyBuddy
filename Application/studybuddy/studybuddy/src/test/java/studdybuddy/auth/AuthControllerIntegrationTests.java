package studdybuddy.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;
import studdybuddy.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

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
    void registerCreatesStudentWithNormalizedEmailAndHashedPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Alex@Example.COM", "Password123!")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.email").value("alex@example.com"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());

        User savedUser = userRepository.findByEmail("alex@example.com").orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        saveUser("alex@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ALEX@example.com", "Password123!")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("An account with this email already exists"));
    }

    @Test
    void mismatchedPasswordReturnsFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Alex",
                                  "lastName": "Chen",
                                  "email": "alex@example.com",
                                  "password": "Password123!",
                                  "confirmPassword": "Different123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.confirmPassword")
                        .value("Passwords do not match"));
    }

    @Test
    void loginCreatesSessionThatCanReadCurrentUser() throws Exception {
        saveUser("alex@example.com", "Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ALEX@example.com", "Password123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.redirectUrl").value("/profile"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@example.com"));

        mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidLoginReturnsGenericUnauthorizedMessage() throws Exception {
        saveUser("alex@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("alex@example.com", "WrongPassword!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void unknownEmailReturnsSameGenericUnauthorizedMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown@example.com", "Password123!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void currentUserWithoutSessionReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void loginAndRegisterPagesArePublicAndContainCsrfMetadata() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-mode=\"login\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Create account")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/matches\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/groups\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/sessions\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/profile\"")));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-mode=\"register\"")));
    }

    @Test
    @WithMockUser(username = "alex@example.com")
    void profilePageContainsLogoutControl() throws Exception {
        saveUser("alex@example.com", "Password123!");
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<dt>Name</dt><dd>Alex Chen</dd>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-logout-button")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-navigation-logout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/matches\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/groups\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/sessions\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/profile\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Create account"))));
    }

    @Test
    void protectedPageRedirectsAnonymousUserToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                        "/login"
                ));
    }

    private void saveUser(String email, String password) {
        userRepository.save(new User(
                "Alex",
                "Chen",
                email,
                passwordEncoder.encode(password),
                UserRole.STUDENT
        ));
    }

    private String registerJson(String email, String password) {
        return """
                {
                  "firstName": "Alex",
                  "lastName": "Chen",
                  "email": "%s",
                  "password": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(email, password, password);
    }

    private String loginJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}
