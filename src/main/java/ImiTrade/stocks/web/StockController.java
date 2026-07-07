package ImiTrade.stocks.web;

import ImiTrade.common.web.ApiResponse;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockLogoResolver;
import ImiTrade.stocks.domain.StockService;
import ImiTrade.stocks.dto.StockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secured read-only stock endpoints (require a valid JWT).
 *
 * <p>{@code /api/v1/stocks/**} is not on the public list, so every method here is
 * covered by the default "authenticated" rule in {@code SecurityFilterChainConfig}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stocks")
@Tag(name = "Stocks", description = "Read-only stock catalog (requires authentication)")
public class StockController {

    private final StockService stockService;
    private final StockLogoResolver stockLogoResolver;

    @Operation(summary = "List stocks", description = "Returns a page of stocks with optional ticker/companyName filters.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Page of stocks",
                    content = @Content(schema = @Schema(implementation = StockResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)),
                    headers = @Header(name = "WWW-Authenticate"))
    })
    @GetMapping
    public ResponseEntity<Page<StockResponse>> getStocks(
            @RequestParam(name = "ticker", required = false) String ticker,
            @RequestParam(name = "companyName", required = false) String companyName,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Stock> stocks = stockService.getStocks(ticker, companyName, pageable);
        return ResponseEntity.ok(stocks.map(stock -> StockResponse.from(stock, stockLogoResolver.resolve(stock.getTicker()))));
    }

    @Operation(summary = "Get stock by id", description = "Returns the stock with the given id.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Stock found",
                    content = @Content(schema = @Schema(implementation = StockResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)),
                    headers = @Header(name = "WWW-Authenticate")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Stock not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<StockResponse> getStockById(@PathVariable("id") Long id) {
        Stock stock = stockService.getStockById(id);
        return ResponseEntity.ok(StockResponse.from(stock, stockLogoResolver.resolve(stock.getTicker())));
    }
}
