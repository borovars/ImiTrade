package ImiTrade.common.exception;

/**
 * Thrown when current market data cannot be obtained from the upstream MOEX ISS
 * API: network failure, non-2xx HTTP response, or a response that could not be
 * interpreted as a price. Mapped to 503 by {@code GlobalExceptionHandler}.
 */
public class MarketDataUnavailableException extends RuntimeException {

    public MarketDataUnavailableException(String message) {
        super(message);
    }

    public MarketDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
