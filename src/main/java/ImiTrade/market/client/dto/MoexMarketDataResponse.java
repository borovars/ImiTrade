package ImiTrade.market.client.dto;

/**
 * Root of the MOEX ISS {@code marketdata} response (compact JSON format).
 *
 * <pre>
 * {
 *   "marketdata": {
 *     "columns": ["SECID", "BOARDID", "LAST"],
 *     "data": [["SBER", "TQBR", 312.45]]
 *   }
 * }
 * </pre>
 *
 * @param marketdata dynamic market-data block for the requested security
 */
public record MoexMarketDataResponse(MoexMarketDataBlock marketdata) {
}
