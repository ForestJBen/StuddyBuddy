package studdybuddy.course;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CourseDataInitializer implements CommandLineRunner {

    private final CourseRepository courseRepository;

    public CourseDataInitializer(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(String... args) {

        addCourseIfMissing(
                "159.101",
                "Applied Programming"
        );

        addCourseIfMissing(
                "159.102",
                "Computer Science Fundamentals"
        );

        addCourseIfMissing(
                "158.120",
                "Fundamentals of Information Technology"
        );

        addCourseIfMissing(
                "159.201",
                "Algorithms and Data Structures"
        );

        addCourseIfMissing(
                "159.202",
                "Computer Architecture"
        );

        addCourseIfMissing(
                "159.221",
                "Database Systems"
        );

        addCourseIfMissing(
                "159.231",
                "Web Development"
        );

        addCourseIfMissing(
                "159.234",
                "Object-Oriented Programming"
        );

        addCourseIfMissing(
                "159.235",
                "Networks and Security"
        );

        addCourseIfMissing(
                "159.251",
                "Software Engineering Design and Construction"
        );

        addCourseIfMissing(
                "159.272",
                "Programming Language Paradigms"
        );

        addCourseIfMissing(
                "159.302",
                "Artificial Intelligence"
        );

        addCourseIfMissing(
                "159.303",
                "Machine Learning"
        );

        addCourseIfMissing(
                "159.305",
                "Computer Graphics"
        );

        addCourseIfMissing(
                "159.312",
                "Advanced Algorithms"
        );

        addCourseIfMissing(
                "159.331",
                "Software Engineering"
        );

        addCourseIfMissing(
                "159.333",
                "Programming Project"
        );

        addCourseIfMissing(
                "159.334",
                "Cloud Computing"
        );

        addCourseIfMissing(
                "159.335",
                "Cybersecurity"
        );

        addCourseIfMissing(
                "159.399",
                "Computer Science Project"
        );
    }

    private void addCourseIfMissing(
            String courseCode,
            String courseName
    ) {

        if (!courseRepository.existsByCourseCode(courseCode)) {

            Course course =
                    new Course(courseCode, courseName);

            courseRepository.save(course);
        }
    }
}
