package studdybuddy.dto;

import java.util.List;

public class SessionSummaryResponse {

    private List<String> topicsCompleted;
    private List<String> remainingProblems;
    private List<String> followUpTasks;
    private String nextSessionGoal;

    public List<String> getTopicsCompleted() {
        return topicsCompleted;
    }

    public void setTopicsCompleted(List<String> topicsCompleted) {
        this.topicsCompleted = topicsCompleted;
    }

    public List<String> getRemainingProblems() {
        return remainingProblems;
    }

    public void setRemainingProblems(List<String> remainingProblems) {
        this.remainingProblems = remainingProblems;
    }

    public List<String> getFollowUpTasks() {
        return followUpTasks;
    }

    public void setFollowUpTasks(List<String> followUpTasks) {
        this.followUpTasks = followUpTasks;
    }

    public String getNextSessionGoal() {
        return nextSessionGoal;
    }

    public void setNextSessionGoal(String nextSessionGoal) {
        this.nextSessionGoal = nextSessionGoal;
    }
}
