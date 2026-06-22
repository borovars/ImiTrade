package ImiTrade.user.web;

import ImiTrade.auth.dto.CurrentUserResponse;
import ImiTrade.common.web.ApiResponse;
import ImiTrade.security.AuthenticatedUser;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secured user endpoints (require a valid JWT).
 *
 * <p>{@code /api/v1/users/**} is not on the public list, so every method here is
 * covered by the default "authenticated" rule in {@code SecurityFilterChainConfig}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Current-user profile (requires authentication)")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current user", description = "Returns the profile of the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Current user profile",
                    content = @Content(schema = @Schema(implementation = CurrentUserResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)),
                    headers = @Header(name = "WWW-Authenticate"))
    })
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userService.getById(principal.userId());
        return ResponseEntity.ok(CurrentUserResponse.from(user));
    }
}
