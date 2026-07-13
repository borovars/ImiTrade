package ImiTrade.market.client;

import ImiTrade.common.exception.InvalidTickerException;
import ImiTrade.common.exception.MarketDataUnavailableException;
import ImiTrade.market.client.dto.MoexMarketDataResponse;
import ImiTrade.market.client.dto.MoexMarketDataRow;
import ImiTrade.market.client.dto.MoexSecuritiesBlock;
import ImiTrade.market.client.dto.MoexSecuritiesRow;
import ImiTrade.market.config.MarketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Thin MOEX ISS HTTP client. Knows only how to talk to MOEX: it builds the request,
 * invokes the API, decodes the {@code marketdata} + {@code securities} response into
 * DTOs, and extracts the current price and lot size. No business logic lives here.
 *
 * <p>Endpoint (no auth required): {@code GET /engines/stock/markets/shares/securities/{ticker}.json}
 * with {@code iss.only=marketdata,securities}, {@code iss.meta=off}, a fixed
 * {@code marketdata} column set {@code SECID,BOARDID,LAST} and a fixed
 * {@code securities} column set {@code SECID,BOARDID,LOTSIZE}. Price and lot size are
 * fetched in a single HTTP call. MOEX's {@code marketdata} block does not expose
 * {@code LOTSIZE}; that field lives in the {@code securities} block.
 *
 * <p>MOEX returns one row per {@code (SECID, BOARDID)} pair, and values differ across
 * boards (e.g. for GAZP the {@code SMAL} lot is 1 while the {@code TQBR} lot is 10).
 * Both the price and the lot size are therefore filtered strictly by the configured
 * board ({@link MarketProperties#boardId()}, default {@code TQBR} — the main T+ board
 * of the Moscow Exchange), with no fallback to other boards.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoexClient {

    private static final String MARKETDATA_COLUMNS_PREFIX = "SECID,BOARDID,";
    private static final String SECURITIES_COLUMNS_PREFIX = "SECID,BOARDID,";

    private final RestClient moexRestClient;
    private final MarketProperties properties;

    /**
     * Returns the current price for the given ticker.
     *
     * <p>Convenience wrapper over {@link #getMarketSnapshot(String)} that drops the
     * lot size. Kept for callers/tests that only care about the price.
     *
     * @param ticker MOEX security identifier, e.g. {@code SBER}
     * @return the last traded price ({@code LAST} marketdata field)
     * @throws InvalidTickerException         if the ticker is blank or unknown to MOEX
     * @throws MarketDataUnavailableException if MOEX is unreachable, returns an error,
     *                                        or has no price for the ticker
     */
    public BigDecimal getCurrentPrice(String ticker) {
        return getMarketSnapshot(ticker).last();
    }

    /**
     * Returns a snapshot of the dynamic market data for the given ticker: the last
     * traded price and the lot size. Both values are read from a single MOEX ISS
     * request.
     *
     * <p>The lot size may be {@code null} when MOEX does not return it (e.g. the
     * ticker is not on a main board); the caller is expected to keep the previously
     * persisted value in that case. The price is always non-null on success.
     *
     * @param ticker MOEX security identifier, e.g. {@code SBER}
     * @return a snapshot with {@code last} (non-null) and {@code lotSize} (nullable)
     * @throws InvalidTickerException         if the ticker is blank or unknown to MOEX
     * @throws MarketDataUnavailableException if MOEX is unreachable, returns an error,
     *                                        or has no price for the ticker
     */
    public MoexSnapshot getMarketSnapshot(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new InvalidTickerException(String.valueOf(ticker));
        }

        String uri = buildUri(ticker);
        log.debug("Requesting marketdata+securities from MOEX: ticker={} uri={}", ticker, uri);

        MoexMarketDataResponse response;
        try {
            response = moexRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(MoexMarketDataResponse.class);
        } catch (ResourceAccessException ex) {
            log.error("MOEX ISS unreachable for ticker={}: {}", ticker, ex.getMessage());
            throw new MarketDataUnavailableException("MOEX ISS unreachable for ticker=" + ticker, ex);
        } catch (RestClientResponseException ex) {
            log.error("MOEX ISS returned {} for ticker={}: {}", ex.getStatusCode(), ticker,
                    ex.getResponseBodyAsString());
            throw new MarketDataUnavailableException(
                    "MOEX ISS returned " + ex.getStatusCode() + " for ticker=" + ticker, ex);
        } catch (RuntimeException ex) {
            log.error("Failed to read MOEX ISS response for ticker={}: {}", ticker, ex.getMessage());
            throw new MarketDataUnavailableException(
                    "Failed to read MOEX ISS response for ticker=" + ticker, ex);
        }

        if (response == null || response.marketdata() == null) {
            throw new MarketDataUnavailableException("Empty MOEX ISS response for ticker=" + ticker);
        }

        List<MoexMarketDataRow> rows = response.marketdata().data();
        if (rows == null || rows.isEmpty()) {
            log.warn("MOEX ISS returned no marketdata rows for ticker={}", ticker);
            throw new InvalidTickerException(ticker);
        }

        BigDecimal price = rows.stream()
                .filter(row -> properties.boardId().equals(row.boardid()))
                .map(MoexMarketDataRow::last)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (price == null) {
            log.warn("MOEX ISS returned no price for ticker={}", ticker);
            throw new MarketDataUnavailableException("No current price for ticker=" + ticker);
        }

        Integer lotSize = extractLotSize(response.securities());

        log.info("Received snapshot from MOEX: ticker={} price={} lotSize={}", ticker, price, lotSize);
        return new MoexSnapshot(price, lotSize);
    }

    /**
     * Picks the lot size from the configured board ({@link MarketProperties#boardId()},
     * default {@code TQBR}) in the {@code securities} block, if present.
     *
     * <p>MOEX returns one row per {@code (SECID, BOARDID)} pair and the lot size
     * differs across boards (e.g. GAZP: {@code SMAL} lot = 1, {@code TQBR} lot = 10),
     * so the row is selected strictly by board with no fallback. Returns {@code null}
     * when MOEX did not return a lot size for the board — the caller then keeps the
     * persisted value.
     */
    private Integer extractLotSize(MoexSecuritiesBlock securities) {
        if (securities == null || securities.data() == null) {
            return null;
        }
        return securities.data().stream()
                .filter(row -> properties.boardId().equals(row.boardid()))
                .map(MoexSecuritiesRow::lotsize)
                .filter(Objects::nonNull)
                .map(BigDecimal::intValueExact)
                .findFirst()
                .orElse(null);
    }

    private String buildUri(String ticker) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .pathSegment("engines", properties.engine())
                .pathSegment("markets", properties.market())
                .pathSegment("securities", ticker + ".json")
                .queryParam("iss.only", "marketdata,securities")
                .queryParam("iss.meta", "off")
                .queryParam("marketdata.columns", MARKETDATA_COLUMNS_PREFIX + properties.priceField())
                .queryParam("securities.columns", SECURITIES_COLUMNS_PREFIX + properties.lotSizeField())
                .build()
                .toUriString();
    }

    /**
     * Immutable snapshot of the dynamic MOEX data for one ticker.
     *
     * @param last    last traded price; always non-null on a successful fetch
     * @param lotSize shares per lot; {@code null} when MOEX did not return one
     */
    public record MoexSnapshot(BigDecimal last, Integer lotSize) {
    }
}
