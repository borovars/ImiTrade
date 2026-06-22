package ImiTrade.common.exception;

/**
 * Base class for domain conflicts (HTTP 409) where a requested unique resource
 * already exists (e.g. duplicate email / username on registration).
 */
public abstract class ResourceAlreadyExistsException extends RuntimeException {

    protected ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
