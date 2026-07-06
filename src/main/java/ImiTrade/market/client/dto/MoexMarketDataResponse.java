package ImiTrade.market.client.dto;

/**
 * Root of the MOEX ISS response (compact JSON format).
 *
 * <p>Both the {@code marketdata} and {@code securities} blocks are requested in a
 * single call ({@code iss.only=marketdata,securities}), so the response carries
 * both. Example:
 *
 * <pre>
 * {
 *   "marketdata": {
 *     "columns": ["SECID", "BOARDID", "LAST"],
 *     "data": [["SBER", "TQBR", 312.45]]
 *   },
 *   "securities": {
 *     "columns": ["SECID", "BOARDID", "LOTSIZE"],
 *     "data": [["SBER", "TQBR", 10]]
 *   }
 * }
 * </pre>
 *
 * <p>{@code securities} may be {@code null} when a caller (or test fixture) only
 * requested the {@code marketdata} block — the client treats a missing lot size
 * as "keep the persisted value".
 *
 * @param marketdata dynamic market-data block for the requested security
 * @param securities static securities block (lot size etc.); nullable
 */
public record MoexMarketDataResponse(
        MoexMarketDataBlock marketdata,
        MoexSecuritiesBlock securities
) {
    /**
     * Backwards-compatible constructor for responses that carry only the
     * {@code marketdata} block (e.g. tests). {@code securities} defaults to
     * {@code null}.
     */
    public MoexMarketDataResponse(MoexMarketDataBlock marketdata) {
        this(marketdata, null);
    }
}
