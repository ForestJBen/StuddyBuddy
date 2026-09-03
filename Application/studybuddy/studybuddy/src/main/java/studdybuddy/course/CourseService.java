package studdybuddy.course;

import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdybuddy.course.dto.CourseRequest;
import studdybuddy.course.dto.CourseResponse;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll(Sort.by("courseCode").ascending())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CourseResponse getCourseById(Long id) {
        return toResponse(findCourseById(id));
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        String courseCode = normalizeCourseCode(request.courseCode());
        String courseName = normalizeText(request.courseName());

        if (courseRepository.existsByCourseCode(courseCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A course with this course code already exists"
            );
        }

        Course course = new Course(courseCode, courseName);
        Course savedCourse = courseRepository.save(course);
        return toResponse(savedCourse);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = findCourseById(id);
        String courseCode = normalizeCourseCode(request.courseCode());
        String courseName = normalizeText(request.courseName());

        courseRepository.findByCourseCode(courseCode)
                .filter(existingCourse -> !existingCourse.getId().equals(id))
                .ifPresent(existingCourse -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "A course with this course code already exists"
                    );
                });

        course.updateDetails(courseCode, courseName);
        return toResponse(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = findCourseById(id);
        courseRepository.delete(course);
    }

    private Course findCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Course not found"
                ));
    }

    private String normalizeCourseCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value.trim();
    }

    private CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCourseCode(),
                course.getCourseName()
        );
    }
}