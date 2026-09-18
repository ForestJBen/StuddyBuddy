package studdybuddy.profile.dto;

import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
        @NotBlank(message = "Programme is required")
        @Size(max = 100, message = "Programme must be at most 100 characters")
        String programme,

        @NotNull(message = "Year is required")
        @Min(value = 1, message = "Year must be between 1 and 10")
        @Max(value = 10, message = "Year must be between 1 and 10")
        Integer year,

        @NotNull(message = "Course selection is required")
        List<@NotNull(message = "Course ID must not be null") Long> courseIds,

        @Size(max = 500, message = "Goals must be at most 500 characters")
        String goals,

        @Size(max = 100, message = "Study style must be at most 100 characters")
        String studyStyle,

        @Size(max = 500, message = "Availability must be at most 500 characters")
        String availability
) {
}