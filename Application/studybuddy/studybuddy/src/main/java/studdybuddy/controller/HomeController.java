package studdybuddy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import studdybuddy.auth.AuthService;

@Controller
public class HomeController {

    private final AuthService authService;

    public HomeController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String showHomePage() {
        return "index";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @GetMapping("/profile")
    public String showProfilePage(Authentication authentication, Model model) {
        var user = authService.getByEmail(authentication.getName());
        model.addAttribute("profileDisplayName", user.firstName() + " " + user.lastName());
        return "profile";
    }

    @GetMapping("/sessions")
    public String showSessionsPage() {return "sessions";}

    @GetMapping("/matches")
    public String showMatchesPage() {return "matches";}

    @GetMapping("/messages/{userId}")
    public String showMessagesPage() {return "messages";}

    @GetMapping("/groups")
    public String showGroupsPage() {return "groups";}

}
