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

    private final studdybuddy.admin.AuditService audit;
    private final studdybuddy.admin.DeletionGuard guard;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public CourseService(CourseRepository courseRepository, studdybuddy.admin.AuditService audit,
            studdybuddy.admin.DeletionGuard guard, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.courseRepository=courseRepository; this.audit=audit; this.guard=guard; this.jdbc=jdbc;
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
        audit.record("COURSE_CREATE", "course:" + savedCourse.getId());
        return toResponse(savedCourse);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
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

        if (!course.getCourseCode().equals(courseCode)) requireNoGroups(course.getCourseCode());
        course.updateDetails(courseCode, courseName);
        audit.record("COURSE_UPDATE", "course:" + id);
        return toResponse(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        requireNoGroups(course.getCourseCode());
        guard.requireUnreferenced("courses", id);
        courseRepository.delete(course);
        courseRepository.flush();
        audit.record("COURSE_DELETE", "course:" + id);
    }

    private void requireNoGroups(String code) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM study_groups WHERE UPPER(course) = ?", Long.class, code) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course is used by study groups; its code cannot be changed and it cannot be deleted.");
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