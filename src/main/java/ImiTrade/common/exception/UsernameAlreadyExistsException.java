package ImiTrade.common.exception;

public class UsernameAlreadyExistsException extends ResourceAlreadyExistsException {

    public UsernameAlreadyExistsException(String username) {
        super("User with username '" + username + "' already exists");
    }
}
