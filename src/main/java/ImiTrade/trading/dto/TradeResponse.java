package ImiTrade.trading.dto;

import ImiTrade.transaction.domain.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Result of a buy or sell trade. Built from the persisted {@link Transaction}
 * together with the stock ticker (which is not stored on the transaction row).
 *
 * <p>{@code quantity} is the actual share count that was traded (the source of
 * truth, stored on the transaction). {@code lots} and {@code lotSize} are derived
 * for display convenience: {@code lots = quantity / lotSize}.
 */
@Schema(name = "TradeResponse", description = "Result of a buy/sell trade")
public record TradeResponse(
        Long transactionId,
        String stockTicker,
        String type,
        Integer quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        Integer lotSize,
        Integer lots
) {
    public static TradeResponse of(Transaction tx, String ticker, Integer lotSize) {
        Integer lots = (lotSize != null && lotSize > 0)
                ? tx.getQuantity() / lotSize
                : null;
        return new TradeResponse(
                tx.getId(),
                ticker,
                tx.getType().name(),
                tx.getQuantity(),
                tx.getPrice(),
                tx.getTotalAmount(),
                lotSize,
                lots
        );
    }
}
