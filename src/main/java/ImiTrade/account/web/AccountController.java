package ImiTrade.account.web;

import ImiTrade.account.dto.AccountResponse;
import ImiTrade.account.domain.AccountService;
import ImiTrade.common.web.ApiResponse;
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

/**
 * Secured read-only account endpoint (requires a valid JWT).
 *
 * <p>{@code /api/v1/account} is not on the public list, so it is covered by the default
 * "authenticated" rule in {@code SecurityFilterChainConfig}. A user can only see their
 * own account summary.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "Current user's account summary (requires authentication)")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Get current account summary",
            description = "Returns the authenticated user's account summary with balance, live portfolio value, "
                    + "total assets, unrealized profit/loss and number of positions. Used as the main screen.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Account summary",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)),
                    headers = @Header(name = "WWW-Authenticate"))
    })
    @GetMapping
    public ResponseEntity<AccountResponse> getAccount(@AuthenticationPrincipal AuthenticatedUser principal) {
        log.debug("GET /account user={}", principal.userId());
        return ResponseEntity.ok(accountService.getCurrentAccount(principal.userId()));
    }
}
