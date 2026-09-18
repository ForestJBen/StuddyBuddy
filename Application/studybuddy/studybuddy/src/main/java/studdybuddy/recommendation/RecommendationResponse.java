package studdybuddy.recommendation;

import java.util.List;

public class RecommendationResponse {

    private Long userId;
    private String firstName;
    private String lastName;
    private double matchScore;
    private List<String> sharedCourses;
    private List<String> reasons;

    public RecommendationResponse() {
    }

    public RecommendationResponse(
            Long userId,
            String firstName,
            String lastName,
            double matchScore,
            List<String> sharedCourses,
            List<String> reasons
    ) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.matchScore = matchScore;
        this.sharedCourses = sharedCourses;
        this.reasons = reasons;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public List<String> getSharedCourses() {
        return sharedCourses;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
