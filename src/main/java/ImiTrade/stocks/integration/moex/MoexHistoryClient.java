package ImiTrade.stocks.integration.moex;

import ImiTrade.common.exception.InvalidTickerException;
import ImiTrade.common.exception.MarketDataUnavailableException;
import ImiTrade.market.config.MarketProperties;
import ImiTrade.stocks.integration.moex.dto.MoexCandlesResponse;
import ImiTrade.stocks.integration.moex.dto.MoexCandleRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

/**
 * Thin MOEX ISS history client for the {@code candles.json} endpoint. Knows only how
 * to talk to MOEX: it builds the request, invokes the API, decodes the {@code candles}
 * response into DTOs, and returns the raw rows. No business logic lives here — mapping
 * to {@link ImiTrade.stocks.dto.CandleResponse} is the job of {@link MoexHistoryMapper}.
 *
 * <p>Endpoint (no auth required):
 * {@code GET /engines/stock/markets/shares/securities/{ticker}/candles.json} with
 * {@code iss.only=candles}, {@code iss.meta=off}, a fixed candle column set
 * {@code begin,end,open,close,high,low,value,volume}, plus {@code from}, {@code till}
 * and {@code interval}. The column order is fixed by the request, so each row is
 * decoded positionally by {@link MoexCandleRow}.
 *
 * <p>This mirrors the existing {@link ImiTrade.market.client.MoexClient} error
 * handling: network failures and MOEX error responses are translated to
 * {@link MarketDataUnavailableException}, a blank ticker to
 * {@link InvalidTickerException}. It reuses the same {@code moexRestClient} bean
 * (configured to accept MOEX's {@code text/plain} JSON) and the same
 * {@link MarketProperties} (base url / engine / market).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoexHistoryClient {

    /**
     * Fixed candle column order requested from MOEX. The component order of
     * {@link MoexCandleRow} must match this exactly — both are positional.
     */
    private static final String CANDLES_COLUMNS = "begin,end,open,close,high,low,value,volume";

    private final RestClient moexRestClient;
    private final MarketProperties properties;

    /**
     * Fetches raw candle rows for the given ticker and date range.
     *
     * @param ticker   MOEX security identifier, e.g. {@code SBER}
     * @param from     inclusive range start
     * @param till     inclusive range end
     * @param interval MOEX {@code interval} (bucket size): {@code 10} = 10 min,
     *                 {@code 60} = 1 h, {@code 24} = 1 day
     * @return the raw candle rows in the order MOEX returned them (the caller sorts)
     * @throws InvalidTickerException         if the ticker is blank
     * @throws MarketDataUnavailableException if MOEX is unreachable or returns an error
     */
    public List<MoexCandleRow> getCandles(String ticker, LocalDate from, LocalDate till, int interval) {
        if (ticker == null || ticker.isBlank()) {
            throw new InvalidTickerException(String.valueOf(ticker));
        }

        String uri = buildUri(ticker, from, till, interval);
        log.debug("Requesting candles from MOEX: ticker={} from={} till={} interval={} uri={}",
                ticker, from, till, interval, uri);

        MoexCandlesResponse response;
        try {
            response = moexRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(MoexCandlesResponse.class);
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

        if (response == null || response.candles() == null || response.candles().data() == null) {
            return List.of();
        }
        return response.candles().data();
    }

    private String buildUri(String ticker, LocalDate from, LocalDate till, int interval) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .pathSegment("engines", properties.engine())
                .pathSegment("markets", properties.market())
                .pathSegment("securities", ticker, "candles.json")
                .queryParam("iss.only", "candles")
                .queryParam("iss.meta", "off")
                .queryParam("candles.columns", CANDLES_COLUMNS)
                .queryParam("from", from.toString())
                .queryParam("till", till.toString())
                .queryParam("interval", interval)
                .build()
                .toUriString();
    }
}
