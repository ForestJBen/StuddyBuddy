package studdybuddy.recommendation;

import studdybuddy.course.Course;
import studdybuddy.profile.Profile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component

public class SimilarityCalculator {

    public double calculateCourseMatch(Profile current, Profile candidate) {

        Set<String> currentCourses = new HashSet<>();
        for (Course course : current.getCourses()) {
            currentCourses.add(course.getCourseCode());
        }

        Set<String> candidateCourses = new HashSet<>();
        for (Course course : candidate.getCourses()) {
            candidateCourses.add(course.getCourseCode());
        }

        if (currentCourses.isEmpty()) {
            return 0.0;
        }

        int shared = 0;

        for (String courseCode : currentCourses) {
            if (candidateCourses.contains(courseCode)) {
                shared++;
            }
        }

        return (double) shared / currentCourses.size();
    }


    public double calculateStudyStyleMatch(Profile current, Profile candidate) {

        String currentStyle = current.getStudyStyle();
        String candidateStyle = candidate.getStudyStyle();

        if (currentStyle == null || candidateStyle == null) {
            return 0.0;
        }

        if (currentStyle.equalsIgnoreCase(candidateStyle)) {
            return 1.0;
        }

        return 0.0;
    }


    public double calculateYearSimilarity(Profile current, Profile candidate) {

        if (current.getYear() == null || candidate.getYear() == null) {
            return 0.0;
        }

        int difference = Math.abs(current.getYear() - candidate.getYear());

        if (difference == 0) {
            return 1.0;
        }

        if (difference == 1) {
            return 0.5;
        }

        return 0.0;
    }

    public double calculateAvailabilityMatch(
            Profile current,
            Profile candidate
    ) {

        String currentAvailability = current.getAvailability();
        String candidateAvailability = candidate.getAvailability();

        if (currentAvailability == null
                || candidateAvailability == null
                || currentAvailability.isBlank()
                || candidateAvailability.isBlank()) {
            return 0.0;
        }

        Set<String> currentTokens =
                new HashSet<>(List.of(
                        currentAvailability
                                .toLowerCase(Locale.ROOT)
                                .replaceAll("[^a-z0-9 ]", " ")
                                .split("\\s+")
                ));

        Set<String> candidateTokens =
                new HashSet<>(List.of(
                        candidateAvailability
                                .toLowerCase(Locale.ROOT)
                                .replaceAll("[^a-z0-9 ]", " ")
                                .split("\\s+")
                ));

        int shared = 0;

        for (String token : currentTokens) {
            if (candidateTokens.contains(token)) {
                shared++;
            }
        }

        int smallerSetSize =
                Math.min(
                        currentTokens.size(),
                        candidateTokens.size()
                );

        if (smallerSetSize == 0) {
            return 0.0;
        }

        return (double) shared / smallerSetSize;
    }


    public double calculateGoalSimilarity(Profile current, Profile candidate) {

        String currentGoals = current.getGoals();
        String candidateGoals = candidate.getGoals();

        if (currentGoals == null || candidateGoals == null
                || currentGoals.isBlank() || candidateGoals.isBlank()) {
            return 0.0;
        }

        Map<String, Integer> vectorA = createWordVector(currentGoals);
        Map<String, Integer> vectorB = createWordVector(candidateGoals);

        return cosineSimilarity(vectorA, vectorB);
    }


    private Map<String, Integer> createWordVector(String text) {

        Map<String, Integer> vector = new HashMap<>();

        String cleanedText = text
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ");

        String[] words = cleanedText.split("\\s+");

        for (String word : words) {
            if (!word.isBlank()) {
                vector.put(word, vector.getOrDefault(word, 0) + 1);
            }
        }

        return vector;
    }


    private double cosineSimilarity(
            Map<String, Integer> vectorA,
            Map<String, Integer> vectorB
    ) {

        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int value : vectorA.values()) {
            magnitudeA += value * value;
        }

        for (int value : vectorB.values()) {
            magnitudeB += value * value;
        }

        for (String word : vectorA.keySet()) {
            if (vectorB.containsKey(word)) {
                dotProduct += vectorA.get(word) * vectorB.get(word);
            }
        }

        if (magnitudeA == 0 || magnitudeB == 0) {
            return 0.0;
        }

        return dotProduct
                / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB));
    }
}