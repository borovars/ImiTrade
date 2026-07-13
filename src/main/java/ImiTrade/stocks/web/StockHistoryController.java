package ImiTrade.stocks.web;

import ImiTrade.common.web.ApiResponse;
import ImiTrade.stocks.domain.StockService;
import ImiTrade.stocks.dto.CandleResponse;
import ImiTrade.stocks.service.HistoryPeriod;
import ImiTrade.stocks.service.StockHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Secured stock price-history endpoint (requires a valid JWT or X-Guest-Token).
 *
 * <p>{@code /api/v1/stocks/**} is not on the public list, so every method here is
 * covered by the default "authenticated" rule in {@code SecurityFilterChainConfig}.
 *
 * <p>The endpoint resolves the ticker against the catalog first (404 for an unknown
 * ticker), then delegates to {@link StockHistoryService} which fetches OHLCV candles
 * from MOEX ISS. No history is persisted, and the frontend never talks to MOEX
 * directly.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stocks")
@Tag(name = "Stocks", description = "Read-only stock catalog and price history (requires authentication)")
public class StockHistoryController {

    private final StockService stockService;
    private final StockHistoryService stockHistoryService;

    @Operation(summary = "Get stock price history",
            description = "Returns OHLCV candles for the given ticker. Each period maps to a fixed "
                    + "candle interval and default lookback (T-Investments/MOEX style): "
                    + "1D = daily candles, last 3 months; 1W = weekly candles, last 5 months; "
                    + "1M = monthly candles, last 3 years; 1Y = quarterly candles, last 10 years. "
                    + "Defaults to 1D. Sourced live from MOEX ISS; not persisted. "
                    + "Optional `from` (ISO yyyy-MM-dd) shifts the range start further into the past "
                    + "for incremental scroll-to-past on the chart; the candle interval is taken "
                    + "from the period and never changes, so the frontend always merges a single "
                    + "bucket size into the series.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Candle list (may be empty on holidays/weekends)",
                    content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                            schema = @Schema(implementation = CandleResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)),
                    headers = @Header(name = "WWW-Authenticate")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Stock not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
                    description = "MOEX ISS unreachable or returned an error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/{ticker}/history")
    public ResponseEntity<List<CandleResponse>> getStockHistory(
            @PathVariable("ticker") String ticker,
            @RequestParam(name = "period", required = false, defaultValue = "1D") String period,
            @RequestParam(name = "from", required = false) LocalDate from) {
        // Validate the ticker against the catalog before calling MOEX.
        stockService.getStockByTicker(ticker);
        HistoryPeriod parsed = HistoryPeriod.parse(period);
        return ResponseEntity.ok(stockHistoryService.getHistory(ticker, parsed, from));
    }
}
