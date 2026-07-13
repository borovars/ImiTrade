package ImiTrade.portfolio.web;

import ImiTrade.common.web.ApiResponse;
import ImiTrade.portfolio.domain.PortfolioService;
import ImiTrade.portfolio.dto.PortfolioResponse;
import ImiTrade.security.AuthenticatedUser;
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
import org.springframework.web.bind.annotation.RestController;

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
@Tag(name = "Portfolio", description = "Current user's portfolio snapshot (requires authentication)")
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
}
