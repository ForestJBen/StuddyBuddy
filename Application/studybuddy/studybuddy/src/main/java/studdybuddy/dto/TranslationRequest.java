package studdybuddy.dto;

import jakarta.validation.constraints.NotBlank;

public class TranslationRequest {

    @NotBlank(message = "Text must not be empty")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
