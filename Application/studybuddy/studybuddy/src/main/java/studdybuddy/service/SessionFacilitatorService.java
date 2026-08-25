package studdybuddy.service;

import org.springframework.stereotype.Service;
import studdybuddy.dto.SessionPlanRequest;
import studdybuddy.dto.SessionPlanResponse;
import studdybuddy.dto.SessionSummaryRequest;
import studdybuddy.dto.SessionSummaryResponse;
import java.util.List;

@Service
public class SessionFacilitatorService {

    private final AiService aiService;

    public SessionFacilitatorService(AiService aiService) {
        this.aiService = aiService;
    }

    public SessionPlanResponse generateSessionPlan(
            SessionPlanRequest request) {

        return aiService.generateSessionPlan(request);
    }

    public SessionSummaryResponse generateSessionSummary(
            SessionSummaryRequest request) {

        return aiService.generateSessionSummary(request);
    }
}
