package studdybuddy.auth;

import java.util.Map;

public class AuthValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public AuthValidationException(Map<String, String> fieldErrors) {
        super("Please correct the highlighted fields");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
