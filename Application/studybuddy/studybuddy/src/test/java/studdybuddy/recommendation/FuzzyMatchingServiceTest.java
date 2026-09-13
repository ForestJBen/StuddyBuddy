package studdybuddy.recommendation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FuzzyMatchingServiceTest {

    private final FuzzyMatchingService fuzzyMatchingService =
            new FuzzyMatchingService();

    @Test
    void scoreShouldStayWithinValidRange() {

        double score =
                fuzzyMatchingService.calculateFuzzyScore(
                        0.8,
                        0.7,
                        0.9,
                        1.0,
                        0.5
                );

        assertTrue(score >= 0.0 && score <= 100.0);
    }

    @Test
    void highMatchShouldScoreHigherThanLowMatch() {

        double highScore =
                fuzzyMatchingService.calculateFuzzyScore(
                        1.0,
                        0.9,
                        0.9,
                        1.0,
                        1.0
                );

        double lowScore =
                fuzzyMatchingService.calculateFuzzyScore(
                        0.2,
                        0.2,
                        0.1,
                        0.0,
                        0.0
                );

        assertTrue(highScore > lowScore);
    }
}
