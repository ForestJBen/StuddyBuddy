package studdybuddy.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupPostRequest(
        @NotBlank(message = "Post content must not be empty")
        @Size(max = 2000, message = "Post content must be at most 2000 characters")
        String content
) {
}