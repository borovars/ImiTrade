package ImiTrade.transaction.web;

import ImiTrade.common.web.ApiResponse;
import ImiTrade.security.AuthenticatedUser;
import ImiTrade.transaction.domain.TransactionService;
import ImiTrade.transaction.domain.TransactionType;
import ImiTrade.transaction.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secured read-only transaction-history endpoint (requires a valid JWT).
 *
 * <p>{@code /api/v1/transactions/**} is not on the public list, so every method here is
 * covered by the default "authenticated" rule in {@code SecurityFilterChainConfig}.
 * The user is always taken from the authenticated principal, so a user can only ever
 * see their own transactions.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Current user's transaction history (requires authentication)")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "List transactions",
            description = "Returns a page of the current user's transactions, optionally filtered by operation "
                    + "type (BUY/SELL) and/or stock id, sorted by createdAt descending.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Page of transactions",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)),
                    headers = @Header(name = "WWW-Authenticate"))
    })
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(name = "type", required = false) TransactionType type,
            @RequestParam(name = "stockId", required = false) Long stockId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("GET /transactions user={} type={} stockId={}", principal.userId(), type, stockId);
        return ResponseEntity.ok(
                transactionService.getTransactions(principal.userId(), type, stockId, pageable));
    }
}
