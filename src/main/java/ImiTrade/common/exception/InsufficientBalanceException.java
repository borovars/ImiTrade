package ImiTrade.common.exception;

import java.math.BigDecimal;

/**
 * Thrown when a buy trade cannot be executed because the user's balance is
 * lower than the trade total. Mapped to HTTP 400 by {@code GlobalExceptionHandler}.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(BigDecimal balance, BigDecimal required) {
        super("Insufficient balance: available=" + balance + ", required=" + required);
    }
}
