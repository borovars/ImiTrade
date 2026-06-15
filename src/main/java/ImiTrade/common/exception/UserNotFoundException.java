package ImiTrade.common.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String qualifier) {
        super("User not found: " + qualifier);
    }
}
