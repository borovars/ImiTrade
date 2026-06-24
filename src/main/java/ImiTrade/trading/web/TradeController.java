package ImiTrade.trading.web;

import ImiTrade.common.web.ApiResponse;
import ImiTrade.security.AuthenticatedUser;
import ImiTrade.trading.domain.TradeService;
import ImiTrade.trading.dto.BuyStockRequest;
import ImiTrade.trading.dto.SellStockRequest;
import ImiTrade.trading.dto.TradeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secured trading endpoints (require a valid JWT).
 *
 * <p>{@code /api/v1/trades/**} is not on the public list, so every method here is
 * covered by the default "authenticated" rule in {@code SecurityFilterChainConfig}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trades")
@Tag(name = "Trades", description = "Buy and sell stocks (requires authentication)")
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "Buy stocks",
            description = "Buys shares at the stock's current price. Deducts the total from the user balance and updates the portfolio.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Trade executed",
                    content = @Content(schema = @Schema(implementation = TradeResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid quantity / insufficient balance",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)),
                    headers = @Header(name = "WWW-Authenticate")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Stock not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/buy")
    public ResponseEntity<TradeResponse> buy(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @Valid @RequestBody BuyStockRequest request) {
        log.debug("POST /trades/buy user={} stockId={} qty={}", principal.userId(), request.stockId(), request.quantity());
        return ResponseEntity.ok(tradeService.buy(principal.userId(), request));
    }

    @Operation(summary = "Sell stocks",
            description = "Sells shares from the user's portfolio at the stock's current price. Credits the total to the user balance and updates the portfolio.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Trade executed",
                    content = @Content(schema = @Schema(implementation = TradeResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid quantity / insufficient stock quantity",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)),
                    headers = @Header(name = "WWW-Authenticate")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Position/stock not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/sell")
    public ResponseEntity<TradeResponse> sell(@AuthenticationPrincipal AuthenticatedUser principal,
                                              @Valid @RequestBody SellStockRequest request) {
        log.debug("POST /trades/sell user={} stockId={} qty={}", principal.userId(), request.stockId(), request.quantity());
        return ResponseEntity.ok(tradeService.sell(principal.userId(), request));
    }
}
