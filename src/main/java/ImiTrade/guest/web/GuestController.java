package ImiTrade.guest.web;

import ImiTrade.common.web.ApiResponse;
import ImiTrade.guest.domain.GuestService;
import ImiTrade.guest.dto.GuestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public guest endpoint.
 *
 * <p>Whitelisted in {@code SecurityFilterChainConfig} via the {@code /api/v1/guest} matcher.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Guest", description = "Guest user creation")
public class GuestController {

    private final GuestService guestService;

    @Operation(summary = "Create a guest user",
            description = "Creates a new guest account with an initial balance of 5000.00 and returns a guest token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Guest created",
                    content = @Content(schema = @Schema(implementation = GuestResponse.class)))
    })
    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public GuestResponse createGuest() {
        log.debug("POST /guest");
        return guestService.createGuest();
    }
}
