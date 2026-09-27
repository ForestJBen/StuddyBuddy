package studdybuddy.group.dto;

import java.time.Instant;

public record GroupResponse(
        Long id,
        String name,
        String course,
        String description,
        String meetingTime,
        String location,
        Long creatorId,
        String creatorName,
        Instant createdAt,
        long memberCount,
        boolean joined
) {
}