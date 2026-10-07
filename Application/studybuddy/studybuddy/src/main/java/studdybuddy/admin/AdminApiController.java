package studdybuddy.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final AdminService service;
    public AdminApiController(AdminService service) { this.service=service; }
    public record StatusRequest(@NotNull Boolean enabled) {}
    public record MoveRequest(@NotNull @Positive Long destinationGroupId) {}
    @PutMapping("/users/{id}/status")
    public void status(@PathVariable Long id,@Valid @RequestBody StatusRequest request) { service.status(id,request.enabled()); }
    @DeleteMapping("/users/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }
    @PostMapping("/groups/{groupId}/members/{userId}")
    public void add(@PathVariable Long groupId,@PathVariable Long userId) { service.add(groupId,userId); }
    @DeleteMapping("/groups/{groupId}/members/{userId}")
    public void remove(@PathVariable Long groupId,@PathVariable Long userId) { service.remove(groupId,userId); }
    @PostMapping("/groups/{groupId}/members/{userId}/move")
    public void move(@PathVariable Long groupId,@PathVariable Long userId,@Valid @RequestBody MoveRequest request) { service.move(groupId,userId,request.destinationGroupId()); }
}
