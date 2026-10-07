package studdybuddy.admin;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import studdybuddy.user.*;
import studdybuddy.group.*;

@Service
@Transactional
public class AdminService {
    private final UserRepository users;
    private final StudyGroupRepository groups;
    private final GroupMembershipRepository members;
    private final DeletionGuard guard;
    private final AuditService audit;
    public AdminService(UserRepository users, StudyGroupRepository groups, GroupMembershipRepository members,
            DeletionGuard guard, AuditService audit) {
        this.users=users; this.groups=groups; this.members=members; this.guard=guard; this.audit=audit;
    }
    public void status(Long id, boolean enabled) {
        User user=user(id); protectAdmin(user);
        if (user.isEnabled()==enabled) return;
        user.setEnabled(enabled);
        audit.record(enabled ? "USER_ENABLE" : "USER_DISABLE", "user:" + id);
    }
    public void delete(Long id) {
        User user=user(id); protectAdmin(user);
        if(user.isEnabled()) conflict("Disable this account before permanently deleting it.");
        guard.requireUnreferenced("users",id);
        users.delete(user); users.flush();
        audit.record("USER_DELETE", "user:" + id);
    }
    public void add(Long groupId, Long userId) {
        User user=user(userId); var group=group(groupId);
        if(!user.isEnabled()) conflict("Disabled users cannot join groups.");
        if(members.existsByGroup_IdAndUser_Id(groupId,userId)) conflict("User already belongs to this group.");
        members.saveAndFlush(new GroupMembership(group,user));
        audit.record("MEMBER_ADD", "user:"+userId+" group:"+groupId);
    }
    public void remove(Long groupId, Long userId) {
        user(userId); var group=group(groupId); protectCreator(group,userId);
        var membership=membership(groupId,userId);
        members.delete(membership); members.flush();
        audit.record("MEMBER_REMOVE", "user:"+userId+" group:"+groupId);
    }
    public void move(Long sourceId, Long userId, Long destinationId) {
        User user=user(userId);
        if(sourceId.equals(destinationId)) conflict("Choose a different destination group.");
        // Stable lock order avoids opposite moves deadlocking each other.
        var first=group(Math.min(sourceId,destinationId));
        var second=group(Math.max(sourceId,destinationId));
        var source=first.getId().equals(sourceId) ? first : second;
        var destination=first.getId().equals(destinationId) ? first : second;
        protectCreator(source,userId);
        if(!user.isEnabled()) conflict("Disabled users cannot move into groups.");
        var membership=membership(sourceId,userId);
        if(members.existsByGroup_IdAndUser_Id(destinationId,userId)) conflict("User already belongs to the destination group.");
        members.delete(membership); members.flush();
        members.saveAndFlush(new GroupMembership(destination,user));
        audit.record("MEMBER_MOVE", "user:"+userId+" from:"+sourceId+" to:"+destinationId);
    }
    private User user(Long id) { return users.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found")); }
    private StudyGroup group(Long id) { return groups.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Group not found")); }
    private GroupMembership membership(Long groupId,Long userId) {
        return members.findByGroup_IdAndUser_Id(groupId,userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,"User is not a member of this group."));
    }
    private void protectAdmin(User user) { if(user.getRole()==UserRole.ADMIN) conflict("Administrator accounts cannot be disabled or deleted here."); }
    private void protectCreator(StudyGroup group,Long userId) { if(group.getCreator().getId().equals(userId)) conflict("The group creator cannot be removed or moved. Transfer ownership first; ownership transfer is not available here yet."); }
    private void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
