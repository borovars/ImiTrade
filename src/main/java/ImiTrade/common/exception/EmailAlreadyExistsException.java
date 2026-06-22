package ImiTrade.common.exception;

public class EmailAlreadyExistsException extends ResourceAlreadyExistsException {

    public EmailAlreadyExistsException(String email) {
        super("User with email '" + email + "' already exists");
    }
}
