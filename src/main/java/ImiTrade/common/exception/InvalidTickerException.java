package ImiTrade.common.exception;

/**
 * Thrown when a ticker is blank or unknown to MOEX (the upstream returned no
 * {@code marketdata} rows for it). Mapped to 404 by {@code GlobalExceptionHandler}.
 */
public class InvalidTickerException extends RuntimeException {

    public InvalidTickerException(String ticker) {
        super("Unknown ticker: " + ticker);
    }
}
