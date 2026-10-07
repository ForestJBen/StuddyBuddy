package studdybuddy.common.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import studdybuddy.auth.AuthValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrity(org.springframework.dao.DataIntegrityViolationException exception) {
        return ResponseEntity.status(409).body(ApiErrorResponse.general("Operation conflicts with existing or linked records. Refresh and try again."));
    }

    @ExceptionHandler(AuthValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthValidation(
            AuthValidationException exception
    ) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.validation(exception.getFieldErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage() == null
                                ? "Invalid value"
                                : error.getDefaultMessage()
                )
        );

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.validation(fieldErrors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception
    ) {
        String message = exception.getReason();

        if (message == null || message.isBlank()) {
            message = "Request failed";
        }

        return ResponseEntity.status(exception.getStatusCode())
                .body(ApiErrorResponse.general(message));
    }
}
