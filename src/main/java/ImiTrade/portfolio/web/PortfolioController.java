package ImiTrade.portfolio.web;

import ImiTrade.common.web.ApiResponse;
import ImiTrade.portfolio.domain.PortfolioService;
import ImiTrade.portfolio.dto.PortfolioHistoryResponse;
import ImiTrade.portfolio.dto.PortfolioResponse;
import ImiTrade.security.AuthenticatedUser;
import ImiTrade.stocks.service.HistoryPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Secured read-only portfolio endpoint (requires a valid JWT).
 *
 * <p>{@code /api/v1/portfolio/**} is not on the public list, so every method here is
 * covered by the default "authenticated" rule in {@code SecurityFilterChainConfig}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolio")
@Tag(name = "Portfolio", description = "Current user's portfolio snapshot and value history (requires authentication)")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "Get current portfolio",
            description = "Returns all current positions of the authenticated user with live prices and computed unrealized pnl.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Portfolio snapshot",
                    content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                            schema = @Schema(implementation = PortfolioResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)),
                    headers = @Header(name = "WWW-Authenticate"))
    })
    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getPortfolio(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        log.debug("GET /portfolio user={}", principal.userId());
        return ResponseEntity.ok(portfolioService.getPortfolio(principal.userId()));
    }

    @Operation(summary = "Get portfolio value history",
            description = "Returns the historical market value of the authenticated user's investment "
                    + "portfolio as a time series aligned with stock candle buckets. For each timestamp, "
                    + "value = Σ(quantity_held × close_price) across all positions the user held at that "
                    + "moment. Cash balance is excluded — this is the market value of held positions only. "
                    + "Past holdings are reconstructed by replaying the user's transactions; past prices "
                    + "come from MOEX ISS through the same price-history service as the stock detail chart "
                    + "(no second market-data source). Each period maps to a fixed candle interval and "
                    + "default lookback (1D = daily/3 months, 1W = weekly/5 months, 1M = monthly/3 years, "
                    + "1Y = quarterly/10 years); defaults to 1D. Not persisted. Empty list when the user "
                    + "has never traded.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Portfolio value time series (may be empty for a user with no trades)",
                    content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                            schema = @Schema(implementation = PortfolioHistoryResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)),
                    headers = @Header(name = "WWW-Authenticate")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
                    description = "MOEX ISS unreachable or returned an error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/history")
    public ResponseEntity<List<PortfolioHistoryResponse>> getPortfolioHistory(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(name = "period", required = false, defaultValue = "1D") String period,
            @RequestParam(name = "from", required = false) LocalDate from) {
        HistoryPeriod parsed = HistoryPeriod.parse(period);
        log.debug("GET /portfolio/history user={} period={} from={}", principal.userId(), parsed.code(), from);
        return ResponseEntity.ok(portfolioService.getHistory(principal.userId(), parsed, from));
    }
}
