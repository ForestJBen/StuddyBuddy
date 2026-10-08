package studdybuddy.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import studdybuddy.course.Course;
import studdybuddy.course.CourseRepository;
import studdybuddy.group.GroupMembership;
import studdybuddy.group.GroupMembershipRepository;
import studdybuddy.group.StudyGroup;
import studdybuddy.group.StudyGroupRepository;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;
import studdybuddy.user.UserRole;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardSummaryIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private StudyGroupRepository groups;

    @Autowired
    private GroupMembershipRepository memberships;

    @Autowired
    private AdminAuditRepository audits;

    @BeforeEach
    void clearData() {
        audits.deleteAll();
        memberships.deleteAll();
        groups.deleteAll();
        users.deleteAll();
        courses.deleteAll();
    }

    @Test
    void anonymousUserCannotReadDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void studentCannotReadDashboardSummary() throws Exception {
        saveUser("student@test.com", UserRole.STUDENT, true);

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void adminReceivesAccurateDashboardSummary() throws Exception {
        User admin = saveUser("admin@test.com", UserRole.ADMIN, true);
        User activeStudent = saveUser("active@test.com", UserRole.STUDENT, true);
        saveUser("disabled@test.com", UserRole.STUDENT, false);

        courses.save(new Course("COMP101", "Programming"));
        courses.save(new Course("INFO202", "Information Systems"));

        StudyGroup firstGroup = groups.save(new StudyGroup(
                "Programming group", "COMP101", "Weekly practice", null, null, admin
        ));
        StudyGroup secondGroup = groups.save(new StudyGroup(
                "Information group", "INFO202", "Revision", null, null, activeStudent
        ));

        memberships.save(new GroupMembership(firstGroup, admin));
        memberships.save(new GroupMembership(firstGroup, activeStudent));
        memberships.save(new GroupMembership(secondGroup, activeStudent));

        audits.save(new AdminAudit("admin@test.com", "COURSE_CREATE", "course:1"));
        audits.save(new AdminAudit("admin@test.com", "USER_DISABLE", "user:3"));

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.activeUsers").value(2))
                .andExpect(jsonPath("$.disabledUsers").value(1))
                .andExpect(jsonPath("$.studentUsers").value(2))
                .andExpect(jsonPath("$.adminUsers").value(1))
                .andExpect(jsonPath("$.totalCourses").value(2))
                .andExpect(jsonPath("$.totalGroups").value(2))
                .andExpect(jsonPath("$.totalGroupMemberships").value(3))
                .andExpect(jsonPath("$.totalAuditEvents").value(2));
    }

    private User saveUser(String email, UserRole role, boolean enabled) {
        User user = new User("Test", "User", email, "test-password-hash", role);
        user.setEnabled(enabled);
        return users.save(user);
    }
}