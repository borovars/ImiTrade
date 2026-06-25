package ImiTrade.transaction.dto;

import ImiTrade.transaction.domain.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Public read-only representation of a single transaction for the
 * {@code GET /api/v1/transactions} endpoint. Never exposes the entity directly.
 *
 * <p>The {@code ticker} is resolved from the referenced {@code Stock} and passed in
 * explicitly, mirroring {@code TradeResponse}, because the {@link Transaction} entity
 * stores only {@code stockId}.
 */
@Schema(name = "TransactionResponse", description = "A single buy/sell transaction of the current user")
public record TransactionResponse(
        Long id,
        Long stockId,
        String ticker,
        String type,
        Integer quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        Instant createdAt
) {
    /**
     * Assembles the response from the persisted transaction and the resolved ticker.
     *
     * @param tx     the persisted transaction
     * @param ticker the ticker of the referenced stock (resolved separately)
     */
    public static TransactionResponse from(Transaction tx, String ticker) {
        return new TransactionResponse(
                tx.getId(),
                tx.getStockId(),
                ticker,
                tx.getType().name(),
                tx.getQuantity(),
                tx.getPrice(),
                tx.getTotalAmount(),
                tx.getCreatedAt()
        );
    }
}
