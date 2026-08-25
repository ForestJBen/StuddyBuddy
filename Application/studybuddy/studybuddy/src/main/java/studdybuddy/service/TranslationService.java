package studdybuddy.service;

import org.springframework.stereotype.Service;
import studdybuddy.dto.TranslationRequest;
import studdybuddy.dto.TranslationResponse;

@Service
public class TranslationService {

    private final AiService aiService;

    public TranslationService(AiService aiService) {
        this.aiService = aiService;
    }

    public TranslationResponse translate(TranslationRequest request) {

        TranslationResponse response = new TranslationResponse();

        response.setOriginalText(request.getText());

        String translation =
                aiService.translateToEnglish(request.getText());

        response.setTranslatedText(translation);

        return response;
    }
}