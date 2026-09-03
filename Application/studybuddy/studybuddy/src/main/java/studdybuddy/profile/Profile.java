package studdybuddy.profile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import studdybuddy.course.Course;
import studdybuddy.user.User;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String programme;

    @Column(name = "year_level", nullable = false)
    private Integer year;

    @Column(length = 500)
    private String goals;

    @Column(name = "study_style", length = 100)
    private String studyStyle;

    @Column(length = 500)
    private String availability;

    @ManyToMany
    @JoinTable(
            name = "profile_courses",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_profile_courses_profile_course",
                    columnNames = {"profile_id", "course_id"}
            )
    )
    @OrderBy("courseCode ASC")
    private Set<Course> courses = new LinkedHashSet<>();

    protected Profile() {
    }

    public Profile(User user) {
        this.user = user;
    }

    public void updateDetails(
            String programme,
            Integer year,
            String goals,
            String studyStyle,
            String availability,
            Collection<Course> courses
    ) {
        this.programme = programme;
        this.year = year;
        this.goals = goals;
        this.studyStyle = studyStyle;
        this.availability = availability;
        this.courses.clear();
        this.courses.addAll(courses);
    }

    public Long getId() {
        return id;
    }

    public String getProgramme() {
        return programme;
    }

    public Integer getYear() {
        return year;
    }

    public String getGoals() {
        return goals;
    }

    public String getStudyStyle() {
        return studyStyle;
    }

    public String getAvailability() {
        return availability;
    }

    public Set<Course> getCourses() {
        return courses;
    }
}