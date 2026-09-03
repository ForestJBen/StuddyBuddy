package studdybuddy.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import studdybuddy.dto.SessionPlanRequest;
import studdybuddy.dto.SessionPlanResponse;
import studdybuddy.dto.SessionSummaryRequest;
import studdybuddy.dto.SessionSummaryResponse;
import studdybuddy.dto.TranslationRequest;
import studdybuddy.dto.TranslationResponse;
import studdybuddy.service.SessionFacilitatorService;
import studdybuddy.service.TranslationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final SessionFacilitatorService sessionFacilitatorService;
    private final TranslationService translationService;

    public AiController(
            SessionFacilitatorService sessionFacilitatorService,
            TranslationService translationService) {

        this.sessionFacilitatorService = sessionFacilitatorService;
        this.translationService = translationService;
    }

    @PostMapping("/session-plan")
    public SessionPlanResponse generateSessionPlan(
            @Valid @RequestBody SessionPlanRequest request) {

        return sessionFacilitatorService.generateSessionPlan(request);
    }

    @PostMapping("/translate")
    public TranslationResponse translate(
            @Valid @RequestBody TranslationRequest request) {

        return translationService.translate(request);
    }

    @PostMapping("/session-summary")
    public SessionSummaryResponse generateSessionSummary(
            @Valid @RequestBody SessionSummaryRequest request) {

        return sessionFacilitatorService.generateSessionSummary(request);
    }
}