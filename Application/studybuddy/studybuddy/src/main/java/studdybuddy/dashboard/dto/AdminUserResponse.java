package studdybuddy.dashboard.dto;

import java.time.Instant;
import studdybuddy.user.User;
import studdybuddy.user.UserRole;

public record AdminUserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}