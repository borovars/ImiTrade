package ImiTrade.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standard error envelope returned by every failed API call.
 *
 * <pre>
 * {
 *   "timestamp": "2026-06-15T10:30:00Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "code": "VALIDATION_ERROR",
 *   "message": "Validation failed",
 *   "path": "/api/v1/auth/register",
 *   "details": { "email": "must not be blank" }
 * }
 * </pre>
 */
@Schema(name = "ApiErrorResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        java.util.Map<String, String> details
) {
}
