package ImiTrade.trading.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload for {@code POST /api/v1/trades/sell}.
 */
@Schema(name = "SellStockRequest", description = "Payload for POST /api/v1/trades/sell")
public record SellStockRequest(

        @Schema(description = "Stock id to sell", example = "1")
        @NotNull
        Long stockId,

        @Schema(description = "Number of shares to sell (must be > 0)", example = "5")
        @NotNull
        @Positive
        Integer quantity
) {
}
