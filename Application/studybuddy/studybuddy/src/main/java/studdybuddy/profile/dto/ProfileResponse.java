package studdybuddy.profile.dto;

import java.util.List;
import studdybuddy.course.dto.CourseResponse;

public record ProfileResponse(
        Long id,
        String programme,
        Integer year,
        List<CourseResponse> courses,
        String goals,
        String studyStyle,
        String availability
) {
    public static ProfileResponse empty() {
        return new ProfileResponse(null, "", null, List.of(), "", "", "");
    }
}