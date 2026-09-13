package studdybuddy.recommendation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import studdybuddy.course.Course;
import studdybuddy.profile.Profile;
import studdybuddy.profile.ProfileRepository;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final SimilarityCalculator similarityCalculator;
    private final FuzzyMatchingService fuzzyMatchingService;

    public RecommendationService(
            ProfileRepository profileRepository,
            UserRepository userRepository,
            SimilarityCalculator similarityCalculator,
            FuzzyMatchingService fuzzyMatchingService
    ) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.similarityCalculator = similarityCalculator;
        this.fuzzyMatchingService = fuzzyMatchingService;
    }

    public List<RecommendationResponse> getRecommendations(
            String authenticatedEmail
    ) {

        String normalizedEmail =
                authenticatedEmail.trim().toLowerCase(Locale.ROOT);

        User currentUser = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authentication is required"
                        ));

        Long currentUserId = currentUser.getId();

        Profile currentProfile =
                profileRepository.findByUser_Id(currentUserId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Current user profile not found"
                                ));

        List<Profile> allProfiles = profileRepository.findAll();

        PriorityQueue<RecommendationCandidate> queue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                RecommendationCandidate::getFuzzyScore
                        ).reversed()
                );

        for (Profile candidate : allProfiles) {

            // Do not recommend the current user
            if (candidate.getUser().getId().equals(currentUserId)) {
                continue;
            }

            double courseMatch =
                    similarityCalculator.calculateCourseMatch(
                            currentProfile,
                            candidate
                    );

            // Hard filter:
            // candidate must share at least one course
            if (courseMatch == 0.0) {
                continue;
            }

            double availabilityMatch =
                    similarityCalculator.calculateAvailabilityMatch(
                            currentProfile,
                            candidate
                    );

            double goalSimilarity =
                    similarityCalculator.calculateGoalSimilarity(
                            currentProfile,
                            candidate
                    );

            double studyStyleMatch =
                    similarityCalculator.calculateStudyStyleMatch(
                            currentProfile,
                            candidate
                    );

            double yearSimilarity =
                    similarityCalculator.calculateYearSimilarity(
                            currentProfile,
                            candidate
                    );

            double fuzzyScore =
                    fuzzyMatchingService.calculateFuzzyScore(
                            courseMatch,
                            availabilityMatch,
                            goalSimilarity,
                            studyStyleMatch,
                            yearSimilarity
                    );

            RecommendationCandidate recommendationCandidate =
                    new RecommendationCandidate(
                            candidate,
                            courseMatch,
                            availabilityMatch,
                            goalSimilarity,
                            studyStyleMatch,
                            yearSimilarity,
                            fuzzyScore
                    );

            queue.add(recommendationCandidate);
        }

        List<RecommendationResponse> results = new ArrayList<>();

        while (!queue.isEmpty() && results.size() < 5) {

            RecommendationCandidate candidate = queue.poll();

            results.add(
                    toResponse(
                            currentProfile,
                            candidate
                    )
            );
        }

        return results;
    }

    private RecommendationResponse toResponse(
            Profile currentProfile,
            RecommendationCandidate candidate
    ) {

        Profile candidateProfile = candidate.getProfile();
        User user = candidateProfile.getUser();

        List<String> sharedCourses =
                findSharedCourses(
                        currentProfile,
                        candidateProfile
                );

        List<String> reasons =
                buildReasons(candidate);

        return new RecommendationResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                candidate.getFuzzyScore(),
                sharedCourses,
                reasons
        );
    }

    private List<String> findSharedCourses(
            Profile current,
            Profile candidate
    ) {

        List<String> sharedCourses = new ArrayList<>();

        for (Course currentCourse : current.getCourses()) {

            for (Course candidateCourse : candidate.getCourses()) {

                if (currentCourse.getCourseCode()
                        .equalsIgnoreCase(
                                candidateCourse.getCourseCode()
                        )) {

                    sharedCourses.add(
                            currentCourse.getCourseCode()
                    );
                }
            }
        }

        return sharedCourses;
    }

    private List<String> buildReasons(
            RecommendationCandidate candidate
    ) {

        List<String> reasons = new ArrayList<>();

        if (candidate.getCourseMatch() >= 0.6) {
            reasons.add("Strong course match");
        }

        if (candidate.getAvailabilityMatch() >= 0.6) {
            reasons.add("Good availability overlap");
        }

        if (candidate.getGoalSimilarity() >= 0.6) {
            reasons.add("Similar study goals");
        }

        if (candidate.getStudyStyleMatch() >= 0.6) {
            reasons.add("Similar study style");
        }

        if (candidate.getYearSimilarity() >= 0.6) {
            reasons.add("Similar year level");
        }

        if (reasons.isEmpty()) {
            reasons.add("Shares a course with you");
        }

        return reasons;
    }
}