package studdybuddy.common.api;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        boolean success,
        String message,
        Map<String, String> fieldErrors
) {

    public static ApiErrorResponse general(String message) {
        return new ApiErrorResponse(false, message, Map.of());
    }

    public static ApiErrorResponse validation(Map<String, String> fieldErrors) {
        return new ApiErrorResponse(
                false,
                "Please correct the highlighted fields",
                fieldErrors
        );
    }
}