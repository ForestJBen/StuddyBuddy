package studdybuddy.dto;

import jakarta.validation.constraints.NotBlank;

public class SessionSummaryRequest {

    @NotBlank(message = "Session notes must not be empty")
    private String notes;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}