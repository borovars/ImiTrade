package ImiTrade.common.exception;

public class GuestAlreadyRegisteredException extends ResourceAlreadyExistsException {

    public GuestAlreadyRegisteredException(String token) {
        super("Guest already registered: token=" + token);
    }
}
