package studdybuddy.group;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studdybuddy.group.dto.CreateGroupPostRequest;
import studdybuddy.group.dto.CreateGroupRequest;
import studdybuddy.group.dto.GroupMemberResponse;
import studdybuddy.group.dto.GroupPostResponse;
import studdybuddy.group.dto.GroupResponse;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<GroupResponse> listGroups(Authentication authentication) {
        return groupService.listGroups(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            Authentication authentication,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        GroupResponse response = groupService.createGroup(
                authentication.getName(),
                request
        );

        return ResponseEntity.created(
                URI.create("/api/groups/" + response.id())
        ).body(response);
    }
    @PutMapping("/{groupId}")
    public GroupResponse updateGroup(
            Authentication authentication,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return groupService.updateGroup(
                authentication.getName(),
                groupId,
                request
        );
    }

    @PostMapping("/{groupId}/join")
    public GroupResponse joinGroup(
            Authentication authentication,
            @PathVariable Long groupId
    ) {
        return groupService.joinGroup(authentication.getName(), groupId);
    }

    @DeleteMapping("/{groupId}/leave")
    public GroupResponse leaveGroup(
            Authentication authentication,
            @PathVariable Long groupId
    ) {
        return groupService.leaveGroup(authentication.getName(), groupId);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            Authentication authentication,
            @PathVariable Long groupId
    ) {
        groupService.deleteGroup(authentication.getName(), groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/members")
    public List<GroupMemberResponse> getMembers(@PathVariable Long groupId) {
        return groupService.getMembers(groupId);
    }

    @GetMapping("/{groupId}/posts")
    public List<GroupPostResponse> getPosts(@PathVariable Long groupId) {
        return groupService.getPosts(groupId);
    }

    @PostMapping("/{groupId}/posts")
    public ResponseEntity<GroupPostResponse> createPost(
            Authentication authentication,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupPostRequest request
    ) {
        GroupPostResponse response = groupService.createPost(
                authentication.getName(),
                groupId,
                request
        );

        return ResponseEntity.status(201).body(response);
    }
}