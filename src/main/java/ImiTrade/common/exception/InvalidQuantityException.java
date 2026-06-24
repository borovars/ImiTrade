package ImiTrade.common.exception;

/**
 * Thrown when a trade request carries a non-positive quantity. Mapped to
 * HTTP 400 by {@code GlobalExceptionHandler}.
 */
public class InvalidQuantityException extends RuntimeException {

    public InvalidQuantityException(int quantity) {
        super("Quantity must be positive, got: " + quantity);
    }
}
