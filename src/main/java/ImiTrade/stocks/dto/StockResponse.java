package ImiTrade.stocks.dto;

import ImiTrade.stocks.domain.Stock;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Public representation of a {@link Stock}. Never exposes the entity directly.
 *
 * <p>{@code logoUrl} is not persisted in the database: it is derived from the ticker
 * by {@code StockLogoResolver} (path of an SVG file under {@code static/logos/}) and
 * passed in here by the controller layer. It points to a publicly served static
 * resource, e.g. {@code /logos/SBER.svg}, falling back to {@code /logos/default.svg}
 * when no logo exists for the ticker.
 */
@Schema(name = "StockResponse", description = "A publicly listed stock")
public record StockResponse(
        Long id,
        String ticker,
        String companyName,
        String exchange,
        BigDecimal currentPrice,
        Integer lotSize,
        String description,
        String sector,
        String website,
        String logoUrl
) {
    public static StockResponse from(Stock stock, String logoUrl) {
        return new StockResponse(
                stock.getId(),
                stock.getTicker(),
                stock.getCompanyName(),
                stock.getExchange(),
                stock.getCurrentPrice(),
                stock.getLotSize(),
                stock.getDescription(),
                stock.getSector(),
                stock.getWebsite(),
                logoUrl
        );
    }
}
