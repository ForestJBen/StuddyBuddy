package studdybuddy.profile;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdybuddy.course.Course;
import studdybuddy.course.CourseRepository;
import studdybuddy.course.dto.CourseResponse;
import studdybuddy.profile.dto.ProfileRequest;
import studdybuddy.profile.dto.ProfileResponse;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public ProfileService(
            ProfileRepository profileRepository,
            UserRepository userRepository,
            CourseRepository courseRepository
    ) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public ProfileResponse getCurrentProfile(String authenticatedEmail) {
        User user = findCurrentUser(authenticatedEmail);

        return profileRepository.findByUser_Id(user.getId())
                .map(this::toResponse)
                .orElseGet(ProfileResponse::empty);
    }

    @Transactional
    public ProfileResponse saveCurrentProfile(
            String authenticatedEmail,
            ProfileRequest request
    ) {
        User user = findCurrentUser(authenticatedEmail);
        List<Course> selectedCourses = findSelectedCourses(request.courseIds());

        Profile profile = profileRepository.findByUser_Id(user.getId())
                .orElseGet(() -> new Profile(user));

        profile.updateDetails(
                request.programme().trim(),
                request.year(),
                normalizeOptionalText(request.goals()),
                normalizeOptionalText(request.studyStyle()),
                normalizeOptionalText(request.availability()),
                selectedCourses
        );

        return toResponse(profileRepository.save(profile));
    }

    private User findCurrentUser(String authenticatedEmail) {
        String normalizedEmail = authenticatedEmail.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication is required"
                ));
    }

    private List<Course> findSelectedCourses(List<Long> courseIds) {
        List<Long> uniqueIds = new LinkedHashSet<>(courseIds).stream().toList();
        List<Course> courses = courseRepository.findAllById(uniqueIds);

        if (courses.size() != uniqueIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One or more selected courses do not exist"
            );
        }

        courses.sort(Comparator.comparing(Course::getCourseCode));
        return courses;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ProfileResponse toResponse(Profile profile) {
        List<CourseResponse> courses = profile.getCourses().stream()
                .map(course -> new CourseResponse(
                        course.getId(),
                        course.getCourseCode(),
                        course.getCourseName()
                ))
                .toList();

        return new ProfileResponse(
                profile.getId(),
                profile.getProgramme(),
                profile.getYear(),
                courses,
                emptyIfNull(profile.getGoals()),
                emptyIfNull(profile.getStudyStyle()),
                emptyIfNull(profile.getAvailability())
        );
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}