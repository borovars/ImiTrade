package ImiTrade.common.exception;

public class InvalidGuestTokenException extends RuntimeException {

    public InvalidGuestTokenException() {
        super("Invalid or missing guest token");
    }

    public InvalidGuestTokenException(String token) {
        super("Invalid guest token: " + token);
    }
}
