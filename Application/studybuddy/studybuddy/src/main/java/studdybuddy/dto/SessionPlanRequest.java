package studdybuddy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class SessionPlanRequest {

    @NotBlank(message = "Course must not be empty")
    private String course;

    @NotBlank(message = "Topic must not be empty")
    private String topic;

    @NotBlank(message = "Goal must not be empty")
    private String goal;

    private String studyStyle;

    @Min(value = 1, message = "Group size must be at least 1")
    private int groupSize;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int duration;

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getStudyStyle() {
        return studyStyle;
    }

    public void setStudyStyle(String studyStyle) {
        this.studyStyle = studyStyle;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(int groupSize) {
        this.groupSize = groupSize;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
