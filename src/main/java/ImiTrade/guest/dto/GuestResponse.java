package ImiTrade.guest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "GuestResponse", description = "Guest token and initial balance issued on first visit")
public record GuestResponse(

        @Schema(description = "Unique guest token (store and pass in X-Guest-Token header)", example = "8c33bb2e-8d4b-4e0c-b57d-0dce5f6c4e3f")
        UUID guestToken,

        @Schema(description = "Initial virtual balance", example = "100000.0000")
        BigDecimal balance
) {
}
