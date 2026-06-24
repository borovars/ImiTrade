package ImiTrade.common.exception;

/**
 * Thrown when a sell trade targets a stock the user does not hold a position in.
 * Mapped to HTTP 404 by {@code GlobalExceptionHandler}.
 */
public class PortfolioPositionNotFoundException extends RuntimeException {

    public PortfolioPositionNotFoundException(Long userId, Long stockId) {
        super("Portfolio position not found for stockId=" + stockId);
    }
}
