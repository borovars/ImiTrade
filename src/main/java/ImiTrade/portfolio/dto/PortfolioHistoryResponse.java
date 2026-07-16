package ImiTrade.portfolio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One point of the current user's portfolio value time series, returned by
 * {@code GET /api/v1/portfolio/history}.
 *
 * <p>{@code value} is the total market value of all positions the user held at
 * {@code time}: {@code Σ(quantity_held × close_price)}. It is computed in memory
 * by replaying the user's transactions and is never persisted. Cash balance is
 * intentionally excluded — this is the market value of the investment portfolio
 * only, mirroring the {@code portfolioValue} aggregate of {@code AccountService}
 * but reconstructed at past timestamps.
 *
 * @param time  start of the candle bucket (UTC), aligned with stock candles
 * @param value total market value of held positions at {@code time}
 */
@Schema(name = "PortfolioHistoryResponse", description = "One point of the portfolio value time series")
public record PortfolioHistoryResponse(
        Instant time,
        BigDecimal value
) {
}
