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
 *       price-field: LAST
 *       lot-size-field: LOTSIZE
 * </pre>
 *
 * <p>Defaults match the public, no-auth MOEX ISS endpoint and the stock/shares engine,
 * so the integration works out of the box and only needs overriding in special cases.
 *
 * @param baseUrl     MOEX ISS root, no trailing slash (e.g. {@code https://iss.moex.com/iss})
 * @param engine      ISS engine segment (e.g. {@code stock})
 * @param market      ISS market segment (e.g. {@code shares})
 * @param priceField  marketdata field that holds the current price (e.g. {@code LAST})
 * @param lotSizeField securities field that holds the lot size (e.g. {@code LOTSIZE})
 */
@ConfigurationProperties(prefix = "app.market.moex")
public record MarketProperties(
        @DefaultValue("https://iss.moex.com/iss") String baseUrl,
        @DefaultValue("stock") String engine,
        @DefaultValue("shares") String market,
        @DefaultValue("LAST") String priceField,
        @DefaultValue("LOTSIZE") String lotSizeField
) {
}
