package studdybuddy.course;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class CourseControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void clearCourses() {
        courseRepository.deleteAll();
    }

    @Test
    void createCourseReturnsCreatedAndNormalizesInput() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseCode": " comp101 ",
                                  "courseName": " Introduction to Programming "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.courseCode").value("COMP101"))
                .andExpect(jsonPath("$.courseName").value("Introduction to Programming"));
    }

    @Test
    void createDuplicateCourseReturnsConflict() throws Exception {
        courseRepository.save(new Course("COMP101", "Introduction to Programming"));

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseCode": "comp101",
                                  "courseName": "Another name"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("A course with this course code already exists"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void createInvalidCourseReturnsSharedValidationError() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseCode": "   ",
                                  "courseName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Please correct the highlighted fields"))
                .andExpect(jsonPath("$.fieldErrors.courseCode")
                        .value("Course code is required"))
                .andExpect(jsonPath("$.fieldErrors.courseName")
                        .value("Course name is required"));
    }

    @Test
    void getAllCoursesReturnsCoursesSortedByCode() throws Exception {
        courseRepository.save(new Course("INFO202", "Information Systems"));
        courseRepository.save(new Course("COMP101", "Introduction to Programming"));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("COMP101"))
                .andExpect(jsonPath("$[1].courseCode").value("INFO202"));
    }

    @Test
    void getMissingCourseReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/courses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Course not found"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void updateAndDeleteCourseChangeStoredData() throws Exception {
        Course savedCourse = courseRepository.save(
                new Course("COMP101", "Introduction to Programming")
        );

        mockMvc.perform(put("/api/courses/{id}", savedCourse.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseCode": "comp102",
                                  "courseName": " Advanced Programming "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("COMP102"))
                .andExpect(jsonPath("$.courseName").value("Advanced Programming"));

        mockMvc.perform(delete("/api/courses/{id}", savedCourse.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/{id}", savedCourse.getId()))
                .andExpect(status().isNotFound());
    }
}