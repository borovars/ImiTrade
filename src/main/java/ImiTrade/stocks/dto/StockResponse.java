package ImiTrade.stocks.dto;

import ImiTrade.stocks.domain.Stock;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public representation of a {@link Stock}. Never exposes the entity directly.
 */
@Schema(name = "StockResponse", description = "A publicly listed stock")
public record StockResponse(
        Long id,
        String ticker,
        String companyName,
        String exchange
) {
    public static StockResponse from(Stock stock) {
        return new StockResponse(
                stock.getId(),
                stock.getTicker(),
                stock.getCompanyName(),
                stock.getExchange()
        );
    }
}
