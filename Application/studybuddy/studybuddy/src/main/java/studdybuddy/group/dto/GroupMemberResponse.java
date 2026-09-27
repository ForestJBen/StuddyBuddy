package studdybuddy.group.dto;

import java.time.Instant;

public record GroupMemberResponse(
        Long userId,
        String firstName,
        String lastName,
        Instant joinedAt,
        boolean creator
) {
}