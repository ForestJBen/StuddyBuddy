package studdybuddy.group;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdybuddy.group.dto.CreateGroupPostRequest;
import studdybuddy.group.dto.CreateGroupRequest;
import studdybuddy.group.dto.GroupMemberResponse;
import studdybuddy.group.dto.GroupPostResponse;
import studdybuddy.group.dto.GroupResponse;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class GroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupPostRepository groupPostRepository;
    private final UserRepository userRepository;

    public GroupService(
            StudyGroupRepository studyGroupRepository,
            GroupMembershipRepository groupMembershipRepository,
            GroupPostRepository groupPostRepository,
            UserRepository userRepository
    ) {
        this.studyGroupRepository = studyGroupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupPostRepository = groupPostRepository;
        this.userRepository = userRepository;
    }

    public List<GroupResponse> listGroups(String authenticatedEmail) {
        User currentUser = findCurrentUser(authenticatedEmail);
        Set<Long> joinedGroupIds = groupMembershipRepository
                .findGroupIdsByUserId(currentUser.getId());

        return studyGroupRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(group -> toGroupResponse(
                        group,
                        joinedGroupIds.contains(group.getId())
                ))
                .toList();
    }

    @Transactional
    public GroupResponse createGroup(
            String authenticatedEmail,
            CreateGroupRequest request
    ) {
        User creator = findCurrentUser(authenticatedEmail);

        StudyGroup group = new StudyGroup(
                request.name().trim(),
                request.course().trim(),
                request.description().trim(),
                normalizeOptionalText(request.meetingTime()),
                normalizeOptionalText(request.location()),
                creator
        );

        StudyGroup savedGroup = studyGroupRepository.save(group);
        groupMembershipRepository.save(new GroupMembership(savedGroup, creator));

        return toGroupResponse(savedGroup, true);
    }

    @Transactional
    public GroupResponse joinGroup(String authenticatedEmail, Long groupId) {
        User currentUser = findCurrentUser(authenticatedEmail);
        StudyGroup group = findGroup(groupId);

        if (groupMembershipRepository.existsByGroup_IdAndUser_Id(
                groupId,
                currentUser.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You are already a member of this group"
            );
        }

        groupMembershipRepository.save(new GroupMembership(group, currentUser));
        return toGroupResponse(group, true);
    }

    @Transactional
    public GroupResponse leaveGroup(String authenticatedEmail, Long groupId) {
        User currentUser = findCurrentUser(authenticatedEmail);
        StudyGroup group = findGroup(groupId);

        if (group.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Group creator cannot leave the group"
            );
        }

        GroupMembership membership = groupMembershipRepository
                .findByGroup_IdAndUser_Id(groupId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "You are not a member of this group"
                ));

        groupMembershipRepository.delete(membership);
        groupMembershipRepository.flush();
        return toGroupResponse(group, false);
    }

    public List<GroupMemberResponse> getMembers(Long groupId) {
        StudyGroup group = findGroup(groupId);

        return groupMembershipRepository.findByGroup_IdOrderByJoinedAtAsc(groupId)
                .stream()
                .map(membership -> new GroupMemberResponse(
                        membership.getUser().getId(),
                        membership.getUser().getFirstName(),
                        membership.getUser().getLastName(),
                        membership.getJoinedAt(),
                        membership.getUser().getId()
                                .equals(group.getCreator().getId())
                ))
                .toList();
    }

    public List<GroupPostResponse> getPosts(Long groupId) {
        findGroup(groupId);

        return groupPostRepository.findByGroup_IdOrderByCreatedAtAsc(groupId)
                .stream()
                .map(this::toPostResponse)
                .toList();
    }

    @Transactional
    public GroupPostResponse createPost(
            String authenticatedEmail,
            Long groupId,
            CreateGroupPostRequest request
    ) {
        User author = findCurrentUser(authenticatedEmail);
        StudyGroup group = findGroup(groupId);

        if (!groupMembershipRepository.existsByGroup_IdAndUser_Id(
                groupId,
                author.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only group members can create posts"
            );
        }

        GroupPost savedPost = groupPostRepository.save(new GroupPost(
                group,
                author,
                request.content().trim()
        ));

        return toPostResponse(savedPost);
    }

    private User findCurrentUser(String authenticatedEmail) {
        String normalizedEmail = authenticatedEmail.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication is required"
                ));
    }

    private StudyGroup findGroup(Long groupId) {
        return studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Study group not found"
                ));
    }

    private GroupResponse toGroupResponse(StudyGroup group, boolean joined) {
        User creator = group.getCreator();

        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getCourse(),
                group.getDescription(),
                emptyIfNull(group.getMeetingTime()),
                emptyIfNull(group.getLocation()),
                creator.getId(),
                creator.getFirstName() + " " + creator.getLastName(),
                group.getCreatedAt(),
                groupMembershipRepository.countByGroup_Id(group.getId()),
                joined
        );
    }

    private GroupPostResponse toPostResponse(GroupPost post) {
        User author = post.getAuthor();

        return new GroupPostResponse(
                post.getId(),
                author.getId(),
                author.getFirstName() + " " + author.getLastName(),
                post.getContent(),
                post.getCreatedAt()
        );
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
