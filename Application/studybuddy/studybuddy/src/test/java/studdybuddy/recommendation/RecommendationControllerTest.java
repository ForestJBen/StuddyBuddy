package studdybuddy.recommendation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    @WithMockUser(username = "fiona@test.com")
    void shouldReturnRecommendationsForAuthenticatedUser() throws Exception {

        RecommendationResponse response =
                new RecommendationResponse(
                        2L,
                        "Alice",
                        "Wang",
                        88.5,
                        List.of("159.302", "159.251"),
                        List.of(
                                "Strong course match",
                                "Similar study goals"
                        )
                );

        when(recommendationService.getRecommendations(
                "fiona@test.com"
        )).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/recommendations")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].firstName").value("Alice"))
                .andExpect(jsonPath("$[0].lastName").value("Wang"))
                .andExpect(jsonPath("$[0].matchScore").value(88.5))
                .andExpect(jsonPath("$[0].sharedCourses[0]").value("159.302"))
                .andExpect(jsonPath("$[0].reasons[0]")
                        .value("Strong course match"));
    }
}