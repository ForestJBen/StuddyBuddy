package studdybuddy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

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
    public String showProfilePage() {
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
