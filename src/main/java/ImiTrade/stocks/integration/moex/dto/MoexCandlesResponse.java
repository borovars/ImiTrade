package ImiTrade.stocks.integration.moex.dto;

/**
 * Root of the MOEX ISS {@code candles.json} response (compact JSON format).
 *
 * <p>The {@code candles} block is requested alone ({@code iss.only=candles}), so the
 * response carries only it. Example:
 *
 * <pre>
 * {
 *   "candles": {
 *     "columns": ["begin", "end", "open", "close", "high", "low", "value", "volume"],
 *     "data": [
 *       ["2026-07-07 10:00:00", "2026-07-07 10:10:00", 312.4, 312.6, 312.8, 312.3, 1234567.8, 3950],
 *       ...
 *     ]
 *   }
 * }
 * </pre>
 *
 * @param candles candle block for the requested security and date range
 */
public record MoexCandlesResponse(
        MoexCandlesBlock candles
) {
}
