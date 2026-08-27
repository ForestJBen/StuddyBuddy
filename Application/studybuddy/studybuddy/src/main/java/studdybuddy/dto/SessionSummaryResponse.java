package studdybuddy.dto;

import java.util.List;

public class SessionSummaryResponse {

    private String sessionOverview;

    private List<String> topicsCompleted;

    private List<String> keyDiscussionPoints;

    private List<String> plannedButNotCompleted;

    private List<String> remainingProblems;

    private List<String> followUpTasks;

    private String nextSessionGoal;

    public String getSessionOverview() {
        return sessionOverview;
    }

    public void setSessionOverview(String sessionOverview) {
        this.sessionOverview = sessionOverview;
    }

    public List<String> getTopicsCompleted() {
        return topicsCompleted;
    }

    public void setTopicsCompleted(List<String> topicsCompleted) {
        this.topicsCompleted = topicsCompleted;
    }

    public List<String> getKeyDiscussionPoints() {
        return keyDiscussionPoints;
    }

    public void setKeyDiscussionPoints(List<String> keyDiscussionPoints) {
        this.keyDiscussionPoints = keyDiscussionPoints;
    }

    public List<String> getPlannedButNotCompleted() {
        return plannedButNotCompleted;
    }

    public void setPlannedButNotCompleted(
            List<String> plannedButNotCompleted) {

        this.plannedButNotCompleted = plannedButNotCompleted;
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
