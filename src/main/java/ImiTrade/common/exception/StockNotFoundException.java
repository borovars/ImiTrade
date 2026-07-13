package ImiTrade.common.exception;

/**
 * Thrown when a stock lookup (by id or ticker) finds no matching row. Mapped to 404 by
 * {@code GlobalExceptionHandler}.
 */
public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(Long id) {
        super("Stock not found");
    }

    public StockNotFoundException(String ticker) {
        super("Stock not found");
    }
}
