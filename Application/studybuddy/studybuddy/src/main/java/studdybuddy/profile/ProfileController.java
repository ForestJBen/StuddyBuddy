package studdybuddy.profile;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studdybuddy.profile.dto.ProfileRequest;
import studdybuddy.profile.dto.ProfileResponse;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse getCurrentProfile(Authentication authentication) {
        return profileService.getCurrentProfile(authentication.getName());
    }

    @PutMapping
    public ProfileResponse saveCurrentProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileRequest request
    ) {
        return profileService.saveCurrentProfile(authentication.getName(), request);
    }
}