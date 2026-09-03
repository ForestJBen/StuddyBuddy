package studdybuddy.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class SessionSummaryRequest {

    @NotBlank(message = "Course must not be empty")
    private String course;

    @NotBlank(message = "Topic must not be empty")
    private String topic;

    @NotBlank(message = "Goal must not be empty")
    private String goal;

    private List<String> agenda;

    private List<String> checklist;

    @NotBlank(message = "Session notes must not be empty")
    private String notes;

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

    public List<String> getAgenda() {
        return agenda;
    }

    public void setAgenda(List<String> agenda) {
        this.agenda = agenda;
    }

    public List<String> getChecklist() {
        return checklist;
    }

    public void setChecklist(List<String> checklist) {
        this.checklist = checklist;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}