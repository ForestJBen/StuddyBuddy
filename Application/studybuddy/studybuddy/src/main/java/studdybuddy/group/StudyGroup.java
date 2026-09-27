package studdybuddy.group;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import studdybuddy.user.User;

@Entity
@Table(name = "study_groups")
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String course;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "meeting_time", length = 100)
    private String meetingTime;

    @Column(length = 200)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StudyGroup() {
        // JPA requires a no-argument constructor when rebuilding an entity from a row.
    }

    public StudyGroup(
            String name,
            String course,
            String description,
            String meetingTime,
            String location,
            User creator
    ) {
        this.name = name;
        this.course = course;
        this.description = description;
        this.meetingTime = meetingTime;
        this.location = location;
        this.creator = creator;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCourse() { return course; }
    public String getDescription() { return description; }
    public String getMeetingTime() { return meetingTime; }
    public String getLocation() { return location; }
    public User getCreator() { return creator; }
    public Instant getCreatedAt() { return createdAt; }
}