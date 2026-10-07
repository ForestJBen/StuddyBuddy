package studdybuddy;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import studdybuddy.admin.*;
import studdybuddy.user.*;
import studdybuddy.course.*;
import studdybuddy.group.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:admin_management_test")
@AutoConfigureMockMvc
@WithMockUser(username="admin@example.com",roles="ADMIN")
class AdminManagementIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CourseRepository courses;
    @Autowired StudyGroupRepository groups;
    @Autowired GroupMembershipRepository members;
    @Autowired AdminAuditRepository audits;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean AuditService audit;
    User admin, student, creator;
    StudyGroup source, destination;
    @BeforeEach void setup() {
        jdbc.execute("DROP TABLE IF EXISTS legacy_user_records");
        members.deleteAll(); groups.deleteAll(); users.deleteAll(); courses.deleteAll(); audits.deleteAll();
        admin=users.save(new User("Demo","Admin","admin@example.com",passwords.encode("Password123!"),UserRole.ADMIN));
        student=users.save(new User("Test","Student","student@example.com",passwords.encode("Password123!"),UserRole.STUDENT));
        creator=users.save(new User("Group","Owner","owner@example.com",passwords.encode("Password123!"),UserRole.STUDENT));
        source=groups.save(new StudyGroup("Source","COMP101","Study",null,null,creator));
        destination=groups.save(new StudyGroup("Destination","COMP102","Study",null,null,creator));
        members.save(new GroupMembership(source,creator));
        members.save(new GroupMembership(destination,creator));
        members.save(new GroupMembership(source,student));
    }
    @Test void courseCrudIsAuditedAndDashboardRendersGroups() throws Exception {
        mvc.perform(post("/api/courses").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"courseCode\":\"DEMO101\",\"courseName\":\"Demo course\"}"))
            .andExpect(status().isCreated());
        var course=courses.findByCourseCode("DEMO101").orElseThrow();
        mvc.perform(put("/api/courses/"+course.getId()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"courseCode\":\"DEMO102\",\"courseName\":\"Renamed\"}"))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/courses/"+course.getId()).with(csrf())).andExpect(status().isNoContent());
        assertThat(audits.findAll()).hasSize(3).allSatisfy(a -> {
            assertThat(a.getActor()).isEqualTo("admin@example.com");
            assertThat(a.getOccurredAt()).isNotNull();
            assertThat(a.getTarget()).isEqualTo("course:"+course.getId());
        });
        mvc.perform(get("/admin")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Group creator · protected")));
    }
    @Test @WithMockUser(username="student@example.com",roles="STUDENT")
    void studentCannotUseAnyAdminMutation() throws Exception {
        mvc.perform(post("/api/courses").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isForbidden());
        mvc.perform(put("/api/courses/1").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isForbidden());
        mvc.perform(delete("/api/courses/1").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/users/1/status").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}")) .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/users/1").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/groups/1/members/1").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/groups/1/members/1").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/groups/1/members/1/move").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"destinationGroupId\":2}")) .andExpect(status().isForbidden());
        assertThat(audits.count()).isZero();
    }
    @Test void mutationsRequireCsrfAndProtectAdmins() throws Exception {
        mvc.perform(delete("/api/admin/users/"+student.getId())).andExpect(status().isForbidden());
        setStatus(admin,false,409);
        mvc.perform(delete("/api/admin/users/"+admin.getId()).with(csrf())).andExpect(status().isConflict());
        assertThat(users.findById(admin.getId()).orElseThrow().isEnabled()).isTrue();
    }
    @Test void disableRevokesExistingSessionAndRestoreAllowsLogin() throws Exception {
        var session=login("student@example.com");
        setStatus(student,false,200);
        mvc.perform(get("/api/auth/me").session(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext((org.springframework.security.core.context.SecurityContext)session.getAttribute("SPRING_SECURITY_CONTEXT")))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"student@example.com\",\"password\":\"Password123!\"}"))
            .andExpect(status().isUnauthorized());
        setStatus(student,true,200);
        login("student@example.com");
    }
    @Test void onlyDisabledUnlinkedUsersCanBeDeletedIncludingLegacyCascadeLinks() throws Exception {
        var unused=users.save(new User("Unused","User","unused@example.com","hash",UserRole.STUDENT));
        mvc.perform(delete("/api/admin/users/"+unused.getId()).with(csrf())).andExpect(status().isConflict());
        setStatus(student,false,200);
        mvc.perform(delete("/api/admin/users/"+student.getId()).with(csrf())).andExpect(status().isConflict());
        setStatus(unused,false,200);
        jdbc.execute("CREATE TABLE legacy_user_records (id BIGINT PRIMARY KEY, user_id BIGINT REFERENCES users(id) ON DELETE CASCADE)");
        jdbc.update("INSERT INTO legacy_user_records VALUES (1, ?)",unused.getId());
        mvc.perform(delete("/api/admin/users/"+unused.getId()).with(csrf())).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM legacy_user_records",Long.class)).isEqualTo(1);
        jdbc.update("DELETE FROM legacy_user_records");
        mvc.perform(delete("/api/admin/users/"+unused.getId()).with(csrf())).andExpect(status().isOk());
        assertThat(users.existsById(unused.getId())).isFalse();
    }
    @Test void linkedCourseCannotBeDeletedOrRenamed() throws Exception {
        var course=courses.save(new Course("COMP101","Programming"));
        mvc.perform(delete("/api/courses/"+course.getId()).with(csrf())).andExpect(status().isConflict());
        mvc.perform(put("/api/courses/"+course.getId()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"courseCode\":\"NEW101\",\"courseName\":\"Programming\"}"))
            .andExpect(status().isConflict());
        assertThat(courses.findById(course.getId()).orElseThrow().getCourseCode()).isEqualTo("COMP101");
    }
    @Test void addRemoveMoveAndCreatorProtection() throws Exception {
        String path="/api/admin/groups/"+source.getId()+"/members/";
        mvc.perform(delete(path+creator.getId()).with(csrf())).andExpect(status().isConflict());
        move(creator,409);
        move(student,200);
        assertThat(members.existsByGroup_IdAndUser_Id(source.getId(),student.getId())).isFalse();
        assertThat(members.existsByGroup_IdAndUser_Id(destination.getId(),student.getId())).isTrue();
        mvc.perform(post(path+student.getId()).with(csrf())).andExpect(status().isOk());
        move(student,409);
        assertThat(members.existsByGroup_IdAndUser_Id(source.getId(),student.getId())).isTrue();
        mvc.perform(delete(path+student.getId()).with(csrf())).andExpect(status().isOk());
        assertThat(audits.count()).isEqualTo(3);
    }
    @Test void failureAfterMembershipWritesRollsBackBothGroupsAndAudit() {
        doThrow(new IllegalStateException("Simulated audit storage failure")).when(audit).record(eq("MEMBER_MOVE"),anyString());
        assertThatThrownBy(() -> move(student,200)).hasRootCauseMessage("Simulated audit storage failure");
        assertThat(members.existsByGroup_IdAndUser_Id(source.getId(),student.getId())).isTrue();
        assertThat(members.existsByGroup_IdAndUser_Id(destination.getId(),student.getId())).isFalse();
        assertThat(audits.count()).isZero();
    }
    private void setStatus(User user,boolean enabled,int expected) throws Exception {
        mvc.perform(put("/api/admin/users/"+user.getId()+"/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"enabled\":"+enabled+"}")).andExpect(status().is(expected));
    }
    private void move(User user,int expected) throws Exception {
        mvc.perform(post("/api/admin/groups/"+source.getId()+"/members/"+user.getId()+"/move").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"destinationGroupId\":"+destination.getId()+"}")).andExpect(status().is(expected));
    }
    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\""+email+"\",\"password\":\"Password123!\"}"))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
}
