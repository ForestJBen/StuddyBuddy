package studdybuddy.recommendation;

import studdybuddy.profile.Profile;

public class RecommendationCandidate {

    private final Profile profile;

    private final double courseMatch;
    private final double availabilityMatch;
    private final double goalSimilarity;
    private final double studyStyleMatch;
    private final double yearSimilarity;

    private final double fuzzyScore;

    public RecommendationCandidate(
            Profile profile,
            double courseMatch,
            double availabilityMatch,
            double goalSimilarity,
            double studyStyleMatch,
            double yearSimilarity,
            double fuzzyScore
    ) {
        this.profile = profile;
        this.courseMatch = courseMatch;
        this.availabilityMatch = availabilityMatch;
        this.goalSimilarity = goalSimilarity;
        this.studyStyleMatch = studyStyleMatch;
        this.yearSimilarity = yearSimilarity;
        this.fuzzyScore = fuzzyScore;
    }

    public Profile getProfile() {
        return profile;
    }

    public double getCourseMatch() {
        return courseMatch;
    }

    public double getAvailabilityMatch() {
        return availabilityMatch;
    }

    public double getGoalSimilarity() {
        return goalSimilarity;
    }

    public double getStudyStyleMatch() {
        return studyStyleMatch;
    }

    public double getYearSimilarity() {
        return yearSimilarity;
    }

    public double getFuzzyScore() {
        return fuzzyScore;
    }
}