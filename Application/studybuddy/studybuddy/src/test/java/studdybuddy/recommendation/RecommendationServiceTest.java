package studdybuddy.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyDouble;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import studdybuddy.profile.Profile;
import studdybuddy.profile.ProfileRepository;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;

class RecommendationServiceTest {

    private ProfileRepository profileRepository;
    private UserRepository userRepository;
    private SimilarityCalculator similarityCalculator;
    private FuzzyMatchingService fuzzyMatchingService;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {

        profileRepository = mock(ProfileRepository.class);
        userRepository = mock(UserRepository.class);
        similarityCalculator = mock(SimilarityCalculator.class);
        fuzzyMatchingService = mock(FuzzyMatchingService.class);

        recommendationService = new RecommendationService(
                profileRepository,
                userRepository,
                similarityCalculator,
                fuzzyMatchingService
        );
    }

    @Test
    void shouldNotRecommendCurrentUser() {

        User currentUser = createUser(1L, "Fiona", "Liu");
        Profile currentProfile = createProfile(currentUser);

        when(userRepository.findByEmail("fiona@test.com"))
                .thenReturn(Optional.of(currentUser));

        when(profileRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(currentProfile));

        when(profileRepository.findAll())
                .thenReturn(List.of(currentProfile));

        List<RecommendationResponse> results =
                recommendationService.getRecommendations(
                        "fiona@test.com"
                );

        assertEquals(0, results.size());
    }

    @Test
    void shouldFilterCandidateWithNoSharedCourse() {

        User currentUser = createUser(1L, "Fiona", "Liu");
        User candidateUser = createUser(2L, "Alice", "Wang");

        Profile currentProfile = createProfile(currentUser);
        Profile candidateProfile = createProfile(candidateUser);

        when(userRepository.findByEmail("fiona@test.com"))
                .thenReturn(Optional.of(currentUser));

        when(profileRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(currentProfile));

        when(profileRepository.findAll())
                .thenReturn(List.of(
                        currentProfile,
                        candidateProfile
                ));

        when(similarityCalculator.calculateCourseMatch(
                currentProfile,
                candidateProfile
        )).thenReturn(0.0);

        List<RecommendationResponse> results =
                recommendationService.getRecommendations(
                        "fiona@test.com"
                );

        assertEquals(0, results.size());
    }

    @Test
    void shouldRankHigherScoreCandidateFirst() {

        User currentUser = createUser(1L, "Fiona", "Liu");
        User aliceUser = createUser(2L, "Alice", "Wang");
        User bobUser = createUser(3L, "Bob", "Smith");

        Profile currentProfile = createProfile(currentUser);
        Profile aliceProfile = createProfile(aliceUser);
        Profile bobProfile = createProfile(bobUser);

        when(userRepository.findByEmail("fiona@test.com"))
                .thenReturn(Optional.of(currentUser));

        when(profileRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(currentProfile));

        when(profileRepository.findAll())
                .thenReturn(List.of(
                        currentProfile,
                        aliceProfile,
                        bobProfile
                ));

        when(similarityCalculator.calculateCourseMatch(
                currentProfile,
                aliceProfile
        )).thenReturn(1.0);

        when(similarityCalculator.calculateCourseMatch(
                currentProfile,
                bobProfile
        )).thenReturn(1.0);

        when(fuzzyMatchingService.calculateFuzzyScore(
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyDouble()
        )).thenReturn(70.0, 90.0);

        List<RecommendationResponse> results =
                recommendationService.getRecommendations(
                        "fiona@test.com"
                );

        assertEquals(2, results.size());

        assertEquals(
                "Bob",
                results.get(0).getFirstName()
        );

        assertEquals(
                90.0,
                results.get(0).getMatchScore()
        );

        assertEquals(
                "Alice",
                results.get(1).getFirstName()
        );
    }

    @Test
    void shouldReturnMaximumFiveRecommendations() {

        User currentUser = createUser(1L, "Fiona", "Liu");
        Profile currentProfile = createProfile(currentUser);

        when(userRepository.findByEmail("fiona@test.com"))
                .thenReturn(Optional.of(currentUser));

        when(profileRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(currentProfile));

        Profile candidate1 =
                createProfile(createUser(2L, "A", "One"));

        Profile candidate2 =
                createProfile(createUser(3L, "B", "Two"));

        Profile candidate3 =
                createProfile(createUser(4L, "C", "Three"));

        Profile candidate4 =
                createProfile(createUser(5L, "D", "Four"));

        Profile candidate5 =
                createProfile(createUser(6L, "E", "Five"));

        Profile candidate6 =
                createProfile(createUser(7L, "F", "Six"));

        when(profileRepository.findAll())
                .thenReturn(List.of(
                        currentProfile,
                        candidate1,
                        candidate2,
                        candidate3,
                        candidate4,
                        candidate5,
                        candidate6
                ));

        when(similarityCalculator.calculateCourseMatch(
                any(Profile.class),
                any(Profile.class)
        )).thenReturn(1.0);

        when(fuzzyMatchingService.calculateFuzzyScore(
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyDouble()
        )).thenReturn(80.0);

        List<RecommendationResponse> results =
                recommendationService.getRecommendations(
                        "fiona@test.com"
                );

        assertEquals(5, results.size());
    }

    private User createUser(
            Long id,
            String firstName,
            String lastName
    ) {

        User user = mock(User.class);

        when(user.getId()).thenReturn(id);
        when(user.getFirstName()).thenReturn(firstName);
        when(user.getLastName()).thenReturn(lastName);

        return user;
    }

    private Profile createProfile(User user) {

        Profile profile = mock(Profile.class);

        when(profile.getUser()).thenReturn(user);
        when(profile.getCourses()).thenReturn(Set.of());

        return profile;
    }
}
