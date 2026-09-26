package studdybuddy.message;

import jakarta.validation.constraints.NotBlank;

public class MessageRequest {

    @NotBlank(message = "Message must not be empty")
    private String content;

    public String getContent() {return content;}

    public void setContent(String content) {this.content = content;}
}
