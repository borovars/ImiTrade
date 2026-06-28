package ImiTrade.market.client;

import ImiTrade.common.exception.InvalidTickerException;
import ImiTrade.common.exception.MarketDataUnavailableException;
import ImiTrade.market.client.dto.MoexMarketDataResponse;
import ImiTrade.market.client.dto.MoexMarketDataRow;
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
 * invokes the API, decodes the {@code marketdata} response into DTOs, and extracts
 * the current price. No business logic lives here.
 *
 * <p>Endpoint (no auth required for {@code marketdata}):
 * {@code GET /engines/stock/markets/shares/securities/{ticker}.json}
 * with {@code iss.only=marketdata}, {@code iss.meta=off} and a fixed column set
 * {@code SECID,BOARDID,LAST}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoexClient {

    private static final String MARKETDATA_COLUMNS_PREFIX = "SECID,BOARDID,";

    private final RestClient moexRestClient;
    private final MarketProperties properties;

    /**
     * Returns the current price for the given ticker.
     *
     * @param ticker MOEX security identifier, e.g. {@code SBER}
     * @return the last traded price ({@code LAST} marketdata field)
     * @throws InvalidTickerException        if the ticker is blank or unknown to MOEX
     * @throws MarketDataUnavailableException if MOEX is unreachable, returns an error,
     *                                       or has no price for the ticker
     */
    public BigDecimal getCurrentPrice(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new InvalidTickerException(String.valueOf(ticker));
        }

        String uri = buildUri(ticker);
        log.debug("Requesting marketdata from MOEX: ticker={} uri={}", ticker, uri);

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
                .map(MoexMarketDataRow::last)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (price == null) {
            log.warn("MOEX ISS returned no price for ticker={}", ticker);
            throw new MarketDataUnavailableException("No current price for ticker=" + ticker);
        }

        log.info("Received price from MOEX: ticker={} price={}", ticker, price);
        return price;
    }

    private String buildUri(String ticker) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .pathSegment("engines", properties.engine())
                .pathSegment("markets", properties.market())
                .pathSegment("securities", ticker + ".json")
                .queryParam("iss.only", "marketdata")
                .queryParam("iss.meta", "off")
                .queryParam("marketdata.columns", MARKETDATA_COLUMNS_PREFIX + properties.priceField())
                .build()
                .toUriString();
    }
}
