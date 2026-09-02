package studdybuddy.auth.dto;

public record AuthResponse(
        boolean success,
        String message,
        UserResponse user,
        String redirectUrl
) {
}
