package studdybuddy.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank(message = "Group name is required")
        @Size(max = 120, message = "Group name must be at most 120 characters")
        String name,

        @NotBlank(message = "Course is required")
        @Size(max = 20, message = "Course must be at most 20 characters")
        String course,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Size(max = 100, message = "Meeting time must be at most 100 characters")
        String meetingTime,

        @Size(max = 200, message = "Location must be at most 200 characters")
        String location
) {
}