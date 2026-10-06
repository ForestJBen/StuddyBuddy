package studdybuddy.group;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;
import studdybuddy.user.UserRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/studybuddy_group_test}",
        "spring.datasource.username=${DB_USERNAME:root}",
        "spring.datasource.password=${DB_PASSWORD:}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class GroupControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private GroupMembershipRepository groupMembershipRepository;

    @Autowired
    private GroupPostRepository groupPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearDatabase() {
        groupPostRepository.deleteAll();
        groupMembershipRepository.deleteAll();
        studyGroupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUpDatabase() {
        clearDatabase();
    }

    @Test
    void unauthenticatedGroupRequestsReturnJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required"));
    }

    @Test
    void createGroupMakesCurrentUserCreatorAndFirstMember() throws Exception {
        LoggedInUser alex = createUserAndLogin("Alex", "alex@example.com");

        mockMvc.perform(post("/api/groups")
                        .with(csrf())
                        .session(alex.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createGroupJson()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name")
                        .value("159.302 Exam Study Group"))
                .andExpect(jsonPath("$.course").value("159.302"))
                .andExpect(jsonPath("$.creatorId").value(alex.user().getId()))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.joined").value(true));

        StudyGroup savedGroup = studyGroupRepository.findAll().get(0);
        assertThat(groupMembershipRepository.existsByGroup_IdAndUser_Id(
                savedGroup.getId(),
                alex.user().getId()
        )).isTrue();
    }

    @Test
    void validationErrorsUseTheSharedFieldErrorsResponse() throws Exception {
        LoggedInUser alex = createUserAndLogin("Alex", "alex@example.com");

        mockMvc.perform(post("/api/groups")
                        .with(csrf())
                        .session(alex.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "course": "",
                                  "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Please correct the highlighted fields"))
                .andExpect(jsonPath("$.fieldErrors.name")
                        .value("Group name is required"))
                .andExpect(jsonPath("$.fieldErrors.course")
                        .value("Course is required"))
                .andExpect(jsonPath("$.fieldErrors.description")
                        .value("Description is required"));
    }

    @Test
    void aUserCanJoinOnceButCannotJoinTheSameGroupTwice() throws Exception {
        LoggedInUser alex = createUserAndLogin("Alex", "alex@example.com");
        LoggedInUser sam = createUserAndLogin("Sam", "sam@example.com");
        StudyGroup group = createGroupWithCreator(alex.user());

        mockMvc.perform(post("/api/groups/{groupId}/join", group.getId())
                        .with(csrf())
                        .session(sam.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(true))
                .andExpect(jsonPath("$.memberCount").value(2));

        mockMvc.perform(post("/api/groups/{groupId}/join", group.getId())
                        .with(csrf())
                        .session(sam.session()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("You are already a member of this group"));

        assertThat(groupMembershipRepository.countByGroup_Id(group.getId()))
                .isEqualTo(2);
    }

    @Test
    void memberCanLeaveButCreatorCannotLeave() throws Exception {
        LoggedInUser alex = createUserAndLogin("Alex", "alex@example.com");
        LoggedInUser sam = createUserAndLogin("Sam", "sam@example.com");
        StudyGroup group = createGroupWithCreator(alex.user());
        groupMembershipRepository.save(new GroupMembership(group, sam.user()));

        mockMvc.perform(delete("/api/groups/{groupId}/leave", group.getId())
                        .with(csrf())
                        .session(sam.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(false))
                .andExpect(jsonPath("$.memberCount").value(1));

        mockMvc.perform(delete("/api/groups/{groupId}/leave", group.getId())
                        .with(csrf())
                        .session(alex.session()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Group creator cannot leave the group"));
    }

    @Test
    void onlyMembersCanPostAndPostsAreReturnedInChronologicalOrder() throws Exception {
        LoggedInUser alex = createUserAndLogin("Alex", "alex@example.com");
        LoggedInUser sam = createUserAndLogin("Sam", "sam@example.com");
        StudyGroup group = createGroupWithCreator(alex.user());

        mockMvc.perform(post("/api/groups/{groupId}/posts", group.getId())
                        .with(csrf())
                        .session(sam.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"I should not be allowed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Only group members can create posts"));

        mockMvc.perform(post("/api/groups/{groupId}/posts", group.getId())
                        .with(csrf())
                        .session(alex.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"First post\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(alex.user().getId()))
                .andExpect(jsonPath("$.content").value("First post"));

        mockMvc.perform(post("/api/groups/{groupId}/posts", group.getId())
                        .with(csrf())
                        .session(alex.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Second post\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/groups/{groupId}/posts", group.getId())
                        .session(sam.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("First post"))
                .andExpect(jsonPath("$[1].content").value("Second post"));
    }

    @Test
    void creatorCanUpdateGroup() throws Exception {
        LoggedInUser alex = createUserAndLogin("Alex", "alex@example.com");
        StudyGroup group = createGroupWithCreator(alex.user());

        mockMvc.perform(put("/api/groups/{groupId}", group.getId())
                        .with(csrf())
                        .session(alex.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateGroupJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(group.getId()))
                .andExpect(jsonPath("$.name").value("Updated 159.302 Group"))
                .andExpect(jsonPath("$.course").value("159.302"))
                .andExpect(jsonPath("$.description").value("Updated revision plan"))
                .andExpect(jsonPath("$.meetingTime").value("Sunday 3:00 PM"))
                .andExpect(jsonPath("$.location").value("Library Level 2"))
                .andExpect(jsonPath("$.creatorId").value(alex.user().getId()))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.joined").value(true));

        StudyGroup updatedGroup = studyGroupRepository.findById(group.getId())
                .orElseThrow();
        assertThat(updatedGroup.getName()).isEqualTo("Updated 159.302 Group");
        assertThat(updatedGroup.getCourse()).isEqualTo("159.302");
        assertThat(updatedGroup.getDescription()).isEqualTo("Updated revision plan");
        assertThat(updatedGroup.getMeetingTime()).isEqualTo("Sunday 3:00 PM");
        assertThat(updatedGroup.getLocation()).isEqualTo("Library Level 2");
    }

    @Test
    void nonCreatorCannotUpdateGroup() throws Exception {
        LoggedInUser alex = createUserAndLogin("Alex", "alex@example.com");
        LoggedInUser sam = createUserAndLogin("Sam", "sam@example.com");
        StudyGroup group = createGroupWithCreator(alex.user());

        mockMvc.perform(put("/api/groups/{groupId}", group.getId())
                        .with(csrf())
                        .session(sam.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateGroupJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Only the group creator can edit this group"));

        StudyGroup unchangedGroup = studyGroupRepository.findById(group.getId())
                .orElseThrow();
        assertThat(unchangedGroup.getName()).isEqualTo("159.302 Exam Study Group");
    }

    @Test
    void creatorCanDeleteGroupWithoutPosts() throws Exception {
        LoggedInUser alex = createUserAndLogin("Alex", "alex@example.com");
        StudyGroup group = createGroupWithCreator(alex.user());

        mockMvc.perform(delete("/api/groups/{groupId}", group.getId())
                        .with(csrf())
                        .session(alex.session()))
                .andExpect(status().isNoContent());

        assertThat(studyGroupRepository.existsById(group.getId())).isFalse();
        assertThat(groupMembershipRepository.existsByGroup_IdAndUser_Id(
                group.getId(), alex.user().getId()
        )).isFalse();
    }

    @Test
    void nonCreatorCannotDeleteGroup() throws Exception {
        LoggedInUser alex = createUserAndLogin("Alex", "alex@example.com");
        LoggedInUser sam = createUserAndLogin("Sam", "sam@example.com");
        StudyGroup group = createGroupWithCreator(alex.user());

        mockMvc.perform(delete("/api/groups/{groupId}", group.getId())
                        .with(csrf())
                        .session(sam.session()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Only the group creator can delete this group"));

        assertThat(studyGroupRepository.existsById(group.getId())).isTrue();
    }

    @Test
    void missingGroupReturns404() throws Exception {
        LoggedInUser alex = createUserAndLogin("Alex", "alex@example.com");
        long missingGroupId = 999_999L;

        mockMvc.perform(put("/api/groups/{groupId}", missingGroupId)
                        .with(csrf())
                        .session(alex.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateGroupJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Study group not found"));

        mockMvc.perform(delete("/api/groups/{groupId}", missingGroupId)
                        .with(csrf())
                        .session(alex.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Study group not found"));
    }

    private StudyGroup createGroupWithCreator(User creator) {
        StudyGroup group = studyGroupRepository.save(new StudyGroup(
                "159.302 Exam Study Group",
                "159.302",
                "Revision for the final exam",
                "Saturday 2:00 PM",
                "Library Level 2",
                creator
        ));
        groupMembershipRepository.save(new GroupMembership(group, creator));
        return group;
    }

    private LoggedInUser createUserAndLogin(String firstName, String email) throws Exception {
        User user = userRepository.save(new User(
                firstName,
                "Student",
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

        MockHttpSession session = (MockHttpSession) loginResult
                .getRequest()
                .getSession(false);

        assertThat(session).isNotNull();
        return new LoggedInUser(user, session);
    }

    private String createGroupJson() {
        return """
                {
                  "name": "159.302 Exam Study Group",
                  "course": "159.302",
                  "description": "Revision for the final exam",
                  "meetingTime": "Saturday 2:00 PM",
                  "location": "Library Level 2"
                }
                """;
    }
    private String updateGroupJson() {
        return """
            {
              "name": "Updated 159.302 Group",
              "course": "159.302",
              "description": "Updated revision plan",
              "meetingTime": "Sunday 3:00 PM",
              "location": "Library Level 2"
            }
            """;
    }

    private record LoggedInUser(User user, MockHttpSession session) {
    }
}