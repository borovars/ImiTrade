package ImiTrade.market.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * MOEX ISS integration configuration bound from {@code app.market.moex.*} in application.yaml.
 *
 * <pre>
 * app:
 *   market:
 *     moex:
 *       base-url: https://iss.moex.com/iss
 *       engine: stock
 *       market: shares
 *       board-id: TQBR
 *       price-field: LAST
 *       lot-size-field: LOTSIZE
 * </pre>
 *
 * <p>Defaults match the public, no-auth MOEX ISS endpoint and the stock/shares engine,
 * so the integration works out of the box and only needs overriding in special cases.
 *
 * <p>{@code boardId} is the MOEX trading board (режим торгов) that both the price and
 * the lot size are read from. MOEX returns one row per {@code (SECID, BOARDID)} pair,
 * and values differ across boards (e.g. for GAZP the {@code SMAL} lot is 1 while the
 * {@code TQBR} lot is 10), so {@code MoexClient} filters strictly by this board with
 * no fallback — {@code TQBR} is the main T+ board of the Moscow Exchange and the
 * source of truth for liquid instruments.
 *
 * @param baseUrl      MOEX ISS root, no trailing slash (e.g. {@code https://iss.moex.com/iss})
 * @param engine       ISS engine segment (e.g. {@code stock})
 * @param market       ISS market segment (e.g. {@code shares})
 * @param boardId      trading board both price and lot size are read from (e.g. {@code TQBR})
 * @param priceField   marketdata field that holds the current price (e.g. {@code LAST})
 * @param lotSizeField securities field that holds the lot size (e.g. {@code LOTSIZE})
 */
@ConfigurationProperties(prefix = "app.market.moex")
public record MarketProperties(
        @DefaultValue("https://iss.moex.com/iss") String baseUrl,
        @DefaultValue("stock") String engine,
        @DefaultValue("shares") String market,
        @DefaultValue("TQBR") String boardId,
        @DefaultValue("LAST") String priceField,
        @DefaultValue("LOTSIZE") String lotSizeField
) {
}
