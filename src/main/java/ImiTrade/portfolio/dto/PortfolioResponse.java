package ImiTrade.portfolio.dto;

import ImiTrade.portfolio.domain.PortfolioPosition;
import ImiTrade.stocks.domain.Stock;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Public read-only representation of a single portfolio line for the
 * {@code GET /api/v1/portfolio} endpoint. Never exposes the entities directly.
 *
 * <p>{@code pnl} is computed in-memory ({@code (currentPrice - averagePrice) * quantity})
 * and is never persisted; this is the only place that calculation surfaces.
 */
@Schema(name = "PortfolioResponse", description = "A single position in the current user's portfolio")
public record PortfolioResponse(
        Long stockId,
        String ticker,
        String companyName,
        Integer quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal pnl,
        Integer lotSize
) {
    /**
     * Assembles the response from the persisted position, the current stock
     * snapshot, and the in-memory pnl value.
     */
    public static PortfolioResponse from(PortfolioPosition position, Stock stock, BigDecimal pnl) {
        return new PortfolioResponse(
                stock.getId(),
                stock.getTicker(),
                stock.getCompanyName(),
                position.getQuantity(),
                position.getAveragePrice(),
                stock.getCurrentPrice(),
                pnl,
                stock.getLotSize()
        );
    }
}
