package studdybuddy.dto;

import java.util.List;

public class SessionPlanResponse {

    private String icebreaker;
    private List<String> agenda;
    private List<String> checklist;
    private List<String> discussionPrompts;

    public String getIcebreaker() {
        return icebreaker;
    }

    public void setIcebreaker(String icebreaker) {
        this.icebreaker = icebreaker;
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

    public List<String> getDiscussionPrompts() {
        return discussionPrompts;
    }

    public void setDiscussionPrompts(List<String> discussionPrompts) {
        this.discussionPrompts = discussionPrompts;
    }
}
