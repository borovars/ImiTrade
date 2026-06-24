package ImiTrade.common.exception;

/**
 * Thrown when a sell trade requests more shares than the user currently holds
 * in the position. Mapped to HTTP 400 by {@code GlobalExceptionHandler}.
 */
public class InsufficientStockQuantityException extends RuntimeException {

    public InsufficientStockQuantityException(int available, int requested) {
        super("Insufficient stock quantity: available=" + available + ", requested=" + requested);
    }
}
