package ImiTrade.stocks.dto;

import ImiTrade.stocks.domain.Stock;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Public representation of a {@link Stock}. Never exposes the entity directly.
 */
@Schema(name = "StockResponse", description = "A publicly listed stock")
public record StockResponse(
        Long id,
        String ticker,
        String companyName,
        String exchange,
        BigDecimal currentPrice,
        Integer lotSize
) {
    public static StockResponse from(Stock stock) {
        return new StockResponse(
                stock.getId(),
                stock.getTicker(),
                stock.getCompanyName(),
                stock.getExchange(),
                stock.getCurrentPrice(),
                stock.getLotSize()
        );
    }
}
