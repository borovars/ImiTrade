package ImiTrade.account.dto;

import ImiTrade.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Public read-only summary of the current user's account for the
 * {@code GET /api/v1/account} endpoint. Used as the application's main screen.
 *
 * <p>Never exposes the entity directly. The monetary values
 * {@code portfolioValue}, {@code totalAssets} and {@code profitLoss} are computed
 * in-memory ({@code scale 4}, {@code HALF_UP}) and are never persisted — this is
 * the only place they surface.
 */
@Schema(name = "AccountResponse", description = "Account summary of the authenticated user")
public record AccountResponse(
        String username,
        String email,
        BigDecimal balance,
        BigDecimal portfolioValue,
        BigDecimal totalAssets,
        BigDecimal profitLoss,
        Integer positionsCount
) {
    /**
     * Assembles the response from the persisted {@link User} and the in-memory
     * computed aggregates.
     *
     * @param user            the authenticated user (source of username, email, balance)
     * @param portfolioValue  {@code Σ(quantity × currentPrice)} of all current positions
     * @param totalAssets     {@code balance + portfolioValue}
     * @param profitLoss      {@code Σ((currentPrice − averagePrice) × quantity)} of all positions
     * @param positionsCount  number of current positions
     */
    public static AccountResponse of(User user, BigDecimal portfolioValue, BigDecimal totalAssets,
                                     BigDecimal profitLoss, Integer positionsCount) {
        return new AccountResponse(
                user.getUsername(),
                user.getEmail(),
                user.getBalance(),
                portfolioValue,
                totalAssets,
                profitLoss,
                positionsCount
        );
    }
}
