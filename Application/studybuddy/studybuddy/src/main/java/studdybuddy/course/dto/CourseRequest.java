package studdybuddy.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank(message = "Course code is required")
        @Size(max = 20, message = "Course code must be at most 20 characters")
        String courseCode,

        @NotBlank(message = "Course name is required")
        @Size(max = 100, message = "Course name must be at most 100 characters")
        String courseName
) {
}