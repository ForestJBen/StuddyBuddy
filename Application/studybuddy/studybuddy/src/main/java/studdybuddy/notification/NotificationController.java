package studdybuddy.notification;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications(Authentication authentication) {
        return notificationService.getCurrentUserNotifications(authentication.getName());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return notificationService.markCurrentUserNotificationAsRead(
                authentication.getName(),
                id
        );
    }
}