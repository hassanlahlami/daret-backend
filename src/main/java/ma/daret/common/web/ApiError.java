package ma.daret.common.web;

import java.util.List;

/**
 * Consistent error envelope returned by every endpoint on failure:
 * {@code {code, message, fieldErrors[]}}.
 */
public record ApiError(String code, String message, List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
