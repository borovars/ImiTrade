package ImiTrade.common.exception;

/**
 * Thrown when a security token (e.g. JWT) is missing, malformed, or expired.
 * Maps to HTTP 401.
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
