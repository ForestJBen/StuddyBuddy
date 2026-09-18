package studdybuddy.recommendation;
import org.springframework.stereotype.Service;

@Service

public class FuzzyMatchingService {

    public double calculateFuzzyScore(
            double courseMatch,
            double availabilityMatch,
            double goalSimilarity,
            double studyStyleMatch,
            double yearSimilarity
    ) {

        double courseLow = low(courseMatch);
        double courseMedium = medium(courseMatch);
        double courseHigh = high(courseMatch);

        double availabilityLow = low(availabilityMatch);
        double availabilityMedium = medium(availabilityMatch);
        double availabilityHigh = high(availabilityMatch);

        double goalLow = low(goalSimilarity);
        double goalMedium = medium(goalSimilarity);
        double goalHigh = high(goalSimilarity);

        double styleHigh = high(studyStyleMatch);

        double yearMedium = medium(yearSimilarity);
        double yearHigh = high(yearSimilarity);

        double poor = 0.0;
        double fair = 0.0;
        double good = 0.0;
        double excellent = 0.0;

        // Excellent rules
        excellent = Math.max(
                excellent,
                min(courseHigh, availabilityHigh, goalHigh)
        );

        excellent = Math.max(
                excellent,
                min(courseHigh, availabilityHigh, styleHigh)
        );

        excellent = Math.max(
                excellent,
                min(courseHigh, goalHigh, styleHigh)
        );

        excellent = Math.max(
                excellent,
                min(availabilityHigh, goalHigh, styleHigh)
        );

        // Good rules
        good = Math.max(
                good,
                min(courseHigh, goalHigh, availabilityMedium)
        );

        good = Math.max(
                good,
                min(courseMedium, availabilityHigh, goalHigh)
        );

        good = Math.max(
                good,
                min(courseHigh, styleHigh, yearHigh)
        );

        good = Math.max(
                good,
                min(courseMedium, goalHigh, styleHigh)
        );

        good = Math.max(
                good,
                min(courseHigh, availabilityHigh, yearMedium)
        );

        // Fair rules
        fair = Math.max(
                fair,
                min(courseHigh, availabilityLow)
        );

        fair = Math.max(
                fair,
                min(courseMedium, goalMedium)
        );

        fair = Math.max(
                fair,
                min(courseMedium, availabilityMedium)
        );

        fair = Math.max(
                fair,
                min(availabilityMedium, goalMedium)
        );

        fair = Math.max(
                fair,
                min(courseHigh, goalLow)
        );
        // Poor rules
        poor = Math.max(
                poor,
                courseLow
        );

        poor = Math.max(
                poor,
                min(availabilityLow, goalLow)
        );

        poor = Math.max(
                poor,
                min(courseLow, goalLow)
        );

        poor = Math.max(
                poor,
                min(courseLow, availabilityLow)
        );

        double ruleBasedScore = defuzzify(
                poor,
                fair,
                good,
                excellent
        );

        double fallbackScore = calculateWeightedFallback(
                courseMatch,
                availabilityMatch,
                goalSimilarity,
                studyStyleMatch,
                yearSimilarity
        );

        if (ruleBasedScore == 0.0) {
            return round(fallbackScore);
        }

        // Combine fuzzy rule output with general weighted score
        double finalScore =
                0.8 * ruleBasedScore
                        + 0.2 * fallbackScore;

        return round(finalScore);
    }


    private double defuzzify(
            double poor,
            double fair,
            double good,
            double excellent
    ) {

        double numerator =
                poor * 20.0
                        + fair * 45.0
                        + good * 70.0
                        + excellent * 90.0;

        double denominator =
                poor + fair + good + excellent;

        if (denominator == 0.0) {
            return 0.0;
        }

        return numerator / denominator;
    }


    private double calculateWeightedFallback(
            double courseMatch,
            double availabilityMatch,
            double goalSimilarity,
            double studyStyleMatch,
            double yearSimilarity
    ) {

        double weighted =
                courseMatch * 0.30
                        + availabilityMatch * 0.25
                        + goalSimilarity * 0.25
                        + studyStyleMatch * 0.15
                        + yearSimilarity * 0.05;

        return weighted * 100.0;
    }


    private double low(double value) {

        value = clamp(value);

        if (value <= 0.3) {
            return 1.0;
        }

        if (value >= 0.5) {
            return 0.0;
        }

        return (0.5 - value) / 0.2;
    }


    private double medium(double value) {

        value = clamp(value);

        if (value <= 0.3 || value >= 0.7) {
            return 0.0;
        }

        if (value == 0.5) {
            return 1.0;
        }

        if (value < 0.5) {
            return (value - 0.3) / 0.2;
        }

        return (0.7 - value) / 0.2;
    }


    private double high(double value) {

        value = clamp(value);

        if (value <= 0.5) {
            return 0.0;
        }

        if (value >= 0.7) {
            return 1.0;
        }

        return (value - 0.5) / 0.2;
    }


    private double min(double a, double b) {
        return Math.min(a, b);
    }


    private double min(double a, double b, double c) {
        return Math.min(a, Math.min(b, c));
    }


    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }


    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}