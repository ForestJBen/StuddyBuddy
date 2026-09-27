package studdybuddy.group.dto;

import java.time.Instant;

public record GroupPostResponse(
        Long id,
        Long authorId,
        String authorName,
        String content,
        Instant createdAt
) {
}