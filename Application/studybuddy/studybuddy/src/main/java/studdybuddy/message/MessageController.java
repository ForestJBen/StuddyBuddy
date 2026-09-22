package studdybuddy.message;

import java.security.Principal;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{userId}")
    public List<MessageResponse> getConversation(
            @PathVariable Long userId, Principal principal) {
        return messageService.getConversation(principal.getName(), userId);
    }

    @PostMapping("/{userId}")
    public MessageResponse sendMessage(
            @PathVariable Long userId,
            @Valid @RequestBody MessageRequest request,
            Principal principal) {

        return messageService.sendMessage(principal.getName(), userId, request.getContent()
        );
    }
}