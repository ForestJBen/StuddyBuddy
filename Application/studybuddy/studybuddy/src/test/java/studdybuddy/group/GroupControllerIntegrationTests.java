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
        "spring.datasource.url=jdbc:h2:mem:groups_api_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
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

    private record LoggedInUser(User user, MockHttpSession session) {
    }
}