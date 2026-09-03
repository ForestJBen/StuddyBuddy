package studdybuddy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import studdybuddy.dto.TranslationRequest;
import studdybuddy.dto.TranslationResponse;
import studdybuddy.service.SessionFacilitatorService;
import studdybuddy.service.TranslationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.security.test.context.support.WithMockUser;
import studdybuddy.dto.SessionPlanRequest;
import studdybuddy.dto.SessionPlanResponse;

import studdybuddy.dto.SessionSummaryRequest;
import studdybuddy.dto.SessionSummaryResponse;

import java.util.List;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranslationService translationService;

    @MockitoBean
    private SessionFacilitatorService sessionFacilitatorService;

    @Test
    @WithMockUser
    void translateValidRequestReturns200() throws Exception {

        TranslationResponse response = new TranslationResponse();
        response.setOriginalText("你好");
        response.setTranslatedText("Hello");

        when(translationService.translate(any(TranslationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/ai/translate")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "text": "你好"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalText").value("你好"))
                .andExpect(jsonPath("$.translatedText").value("Hello"));
    }

    @Test
    @WithMockUser
    void translateEmptyTextReturns400() throws Exception {

        mockMvc.perform(
                        post("/api/ai/translate")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                            {
                              "text": ""
                            }
                            """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Please correct the highlighted fields"))
                .andExpect(jsonPath("$.fieldErrors.text")
                        .value("Text must not be empty"));
    }


    @Test
    @WithMockUser
    void sessionPlanValidRequestReturns200() throws Exception {

        SessionPlanResponse response = new SessionPlanResponse();

        response.setIcebreaker("What is one thing you already know about A*?");
        response.setDiscussionPrompts(List.of(
                "How does A* choose the next node?",
                "What makes a heuristic admissible?"
        ));
        response.setAgenda(List.of(
                "10 min: Review A*",
                "20 min: Solve an example"
        ));
        response.setChecklist(List.of(
                "Understand f(n) = g(n) + h(n)",
                "Practise one A* question"
        ));

        when(sessionFacilitatorService.generateSessionPlan(any(SessionPlanRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/ai/session-plan")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                            {
                              "course": "159.302 Artificial Intelligence",
                              "topic": "A* Search",
                              "goal": "Understand A*",
                              "studyStyle": "Discussion",
                              "groupSize": 4,
                              "duration": 60
                            }
                            """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icebreaker")
                        .value("What is one thing you already know about A*?"))
                .andExpect(jsonPath("$.discussionPrompts[0]")
                        .value("How does A* choose the next node?"))
                .andExpect(jsonPath("$.agenda[0]")
                        .value("10 min: Review A*"))
                .andExpect(jsonPath("$.checklist[0]")
                        .value("Understand f(n) = g(n) + h(n)"));
    }

    @Test
    @WithMockUser
    void sessionPlanInvalidGroupSizeAndDurationReturns400() throws Exception {

        mockMvc.perform(
                        post("/api/ai/session-plan")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                            {
                              "course": "159.302 Artificial Intelligence",
                              "topic": "A* Search",
                              "goal": "Understand A*",
                              "studyStyle": "Discussion",
                              "groupSize": 0,
                              "duration": 0
                            }
                            """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Please correct the highlighted fields"))
                .andExpect(jsonPath("$.fieldErrors.groupSize").exists())
                .andExpect(jsonPath("$.fieldErrors.duration").exists());
    }


    @Test
    @WithMockUser
    void sessionSummaryValidRequestReturns200() throws Exception {

        SessionSummaryResponse response = new SessionSummaryResponse();

        response.setSessionOverview(
                "The group reviewed A* search and completed one exam-style question."
        );

        response.setTopicsCompleted(List.of(
                "Reviewed A* search",
                "Completed one exam-style question"
        ));

        response.setKeyDiscussionPoints(List.of(
                "A* uses f(n) = g(n) + h(n)",
                "Admissible heuristics were discussed"
        ));

        response.setPlannedButNotCompleted(List.of(
                "Complete the second exam-style question"
        ));

        response.setRemainingProblems(List.of(
                "Consistent heuristics are still unclear"
        ));

        response.setFollowUpTasks(List.of(
                "Jinfei will find another consistency example"
        ));

        response.setNextSessionGoal(
                "Clarify consistent heuristics and complete the remaining question"
        );

        when(sessionFacilitatorService.generateSessionSummary(
                any(SessionSummaryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/ai/session-summary")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                            {
                              "course": "159.302 Artificial Intelligence",
                              "topic": "A* Search",
                              "goal": "Understand A*",
                              "agenda": [
                                "Review A*",
                                "Complete two exam-style questions"
                              ],
                              "checklist": [
                                "Understand admissible heuristics",
                                "Understand consistent heuristics"
                              ],
                              "notes": "We reviewed A* and completed one exam-style question. We discussed admissible heuristics but are still confused about consistent heuristics."
                            }
                            """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionOverview")
                        .value("The group reviewed A* search and completed one exam-style question."))
                .andExpect(jsonPath("$.topicsCompleted[0]")
                        .value("Reviewed A* search"))
                .andExpect(jsonPath("$.keyDiscussionPoints[0]")
                        .value("A* uses f(n) = g(n) + h(n)"))
                .andExpect(jsonPath("$.plannedButNotCompleted[0]")
                        .value("Complete the second exam-style question"))
                .andExpect(jsonPath("$.remainingProblems[0]")
                        .value("Consistent heuristics are still unclear"))
                .andExpect(jsonPath("$.followUpTasks[0]")
                        .value("Jinfei will find another consistency example"))
                .andExpect(jsonPath("$.nextSessionGoal")
                        .value("Clarify consistent heuristics and complete the remaining question"));
    }

    @Test
    @WithMockUser
    void aiServiceFailureReturns503() throws Exception {

        when(translationService.translate(any(TranslationRequest.class)))
                .thenThrow(new RuntimeException("AI service is currently unavailable"));

        mockMvc.perform(
                        post("/api/ai/translate")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                            {
                              "text": "你好"
                            }
                            """)
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error")
                        .value("AI service is currently unavailable"));
    }

}


