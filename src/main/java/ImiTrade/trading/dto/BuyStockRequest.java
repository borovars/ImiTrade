package ImiTrade.trading.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload for {@code POST /api/v1/trades/buy}.
 */
@Schema(name = "BuyStockRequest", description = "Payload for POST /api/v1/trades/buy")
public record BuyStockRequest(

        @Schema(description = "Stock id to buy", example = "1")
        @NotNull
        Long stockId,

        @Schema(description = "Number of shares to buy (must be > 0)", example = "10")
        @NotNull
        @Positive
        Integer quantity
) {
}
