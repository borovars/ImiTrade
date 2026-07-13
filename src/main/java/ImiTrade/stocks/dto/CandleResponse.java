package ImiTrade.stocks.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One OHLCV candle of a stock's price history.
 *
 * <p>{@code time} is the candle bucket start as a UTC {@link Instant} (MOEX returns
 * Moscow-time timestamps; {@code MoexHistoryMapper} converts them to UTC). The payload
 * never leaks MOEX-specific fields — only the OHLCV columns the frontend needs.
 *
 * @param time   start of the candle bucket (UTC)
 * @param open   open price
 * @param high   highest price
 * @param low    lowest price
 * @param close  close price
 * @param volume traded volume (number of shares); {@code null} when MOEX omits it
 */
@Schema(name = "CandleResponse", description = "One OHLCV candle of stock price history")
public record CandleResponse(
        Instant time,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume
) {
}
