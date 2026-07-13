package ImiTrade.common.exception;

/**
 * Thrown when login fails because the user does not exist or the password is wrong.
 *
 * <p>Deliberately does NOT distinguish "user not found" from "bad password" to
 * avoid user enumeration.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
