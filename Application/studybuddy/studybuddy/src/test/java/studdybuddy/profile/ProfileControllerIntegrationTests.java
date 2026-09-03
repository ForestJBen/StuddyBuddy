package studdybuddy.profile;

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
import studdybuddy.course.Course;
import studdybuddy.course.CourseRepository;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;
import studdybuddy.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        profileRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUpDatabase() {
        clearDatabase();
    }

    @Test
    void unauthenticatedGetAndPutReturnJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication is required"));

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("Information Sciences", "[]")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void missingProfileReturnsEmptyResponseInsteadOfNotFound() throws Exception {
        MockHttpSession session = createUserAndLogin("alex@example.com");

        mockMvc.perform(get("/api/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.programme").value(""))
                .andExpect(jsonPath("$.year").doesNotExist())
                .andExpect(jsonPath("$.courses").isEmpty())
                .andExpect(jsonPath("$.goals").value(""))
                .andExpect(jsonPath("$.studyStyle").value(""))
                .andExpect(jsonPath("$.availability").value(""));
    }

    @Test
    void putCreatesProfileForAuthenticatedUser() throws Exception {
        MockHttpSession session = createUserAndLogin("alex@example.com");

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(" Information Sciences ", "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.programme").value("Information Sciences"))
                .andExpect(jsonPath("$.courses").isEmpty());

        assertThat(profileRepository.count()).isEqualTo(1);
    }

    @Test
    void secondPutUpdatesTheSameProfileInsteadOfCreatingDuplicate() throws Exception {
        MockHttpSession session = createUserAndLogin("alex@example.com");

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("Original programme", "[]")))
                .andExpect(status().isOk());

        Long originalProfileId = profileRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("Updated programme", "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(originalProfileId))
                .andExpect(jsonPath("$.programme").value("Updated programme"));

        assertThat(profileRepository.count()).isEqualTo(1);
    }

    @Test
    void validCourseIdsReturnCompleteCourseObjects() throws Exception {
        MockHttpSession session = createUserAndLogin("alex@example.com");
        Course course = courseRepository.save(
                new Course("COMP101", "Introduction to Programming")
        );

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "Information Sciences",
                                "[" + course.getId() + "]"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses[0].id").value(course.getId()))
                .andExpect(jsonPath("$.courses[0].courseCode").value("COMP101"))
                .andExpect(jsonPath("$.courses[0].courseName")
                        .value("Introduction to Programming"));
    }

    @Test
    void invalidCourseIdReturnsBadRequestWithoutPartiallySavingChanges() throws Exception {
        MockHttpSession session = createUserAndLogin("alex@example.com");
        Course course = courseRepository.save(new Course("COMP101", "Programming"));

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "Original programme",
                                "[" + course.getId() + "]"
                        )))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "This must not be saved",
                                "[" + course.getId() + ", 999999]"
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("One or more selected courses do not exist"));

        mockMvc.perform(get("/api/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programme").value("Original programme"))
                .andExpect(jsonPath("$.courses.length()").value(1))
                .andExpect(jsonPath("$.courses[0].courseCode").value("COMP101"));
    }

    @Test
    void validationFailuresUseSharedFieldErrorsStructure() throws Exception {
        MockHttpSession session = createUserAndLogin("alex@example.com");

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "programme": "   ",
                                  "year": 11,
                                  "courseIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Please correct the highlighted fields"))
                .andExpect(jsonPath("$.fieldErrors.programme")
                        .value("Programme is required"))
                .andExpect(jsonPath("$.fieldErrors.year")
                        .value("Year must be between 1 and 10"));
    }

    @Test
    void eachUserCanOnlyReadAndModifyTheirOwnProfile() throws Exception {
        MockHttpSession alexSession = createUserAndLogin("alex@example.com");
        MockHttpSession samSession = createUserAndLogin("sam@example.com");

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .session(alexSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("Alex programme", "[]")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profile").session(samSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.programme").value(""));

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .session(samSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson("Sam programme", "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programme").value("Sam programme"));

        mockMvc.perform(get("/api/profile").session(alexSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programme").value("Alex programme"));

        assertThat(profileRepository.count()).isEqualTo(2);
    }

    @Test
    void duplicateCourseIdsCreateOnlyOneJoinTableRow() throws Exception {
        MockHttpSession session = createUserAndLogin("alex@example.com");
        Course course = courseRepository.save(new Course("COMP101", "Programming"));

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson(
                                "Information Sciences",
                                "[" + course.getId() + ", " + course.getId() + "]"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses.length()").value(1));

        Long joinRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM profile_courses",
                Long.class
        );
        assertThat(joinRowCount).isEqualTo(1L);
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

    private String profileJson(String programme, String courseIdsJson) {
        return """
                {
                  "programme": "%s",
                  "year": 2,
                  "courseIds": %s,
                  "goals": "Prepare for exams and improve Java skills",
                  "studyStyle": "Group discussion",
                  "availability": "Weekday evenings"
                }
                """.formatted(programme, courseIdsJson);
    }
}