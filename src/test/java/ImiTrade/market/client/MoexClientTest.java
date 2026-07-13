package ImiTrade.market.client;

import ImiTrade.common.exception.InvalidTickerException;
import ImiTrade.common.exception.MarketDataUnavailableException;
import ImiTrade.market.config.MarketProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link MoexClient}. The HTTP layer is mocked with
 * {@link MockRestServiceServer} bound directly to the {@link RestClient} builder —
 * no Spring context, no real network calls.
 *
 * <p>MOEX serves {@code .json} with {@code Content-Type: text/plain}, so the same
 * extra Jackson converter as in {@code MarketClientConfig} is registered here.
 */
class MoexClientTest {

    private static final String BASE_URL = "https://iss.moex.com/iss";

    private MockRestServiceServer server;
    private MoexClient moexClient;

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter textPlainJsonConverter =
                new MappingJackson2HttpMessageConverter(new ObjectMapper());
        textPlainJsonConverter.setSupportedMediaTypes(List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON));

        RestClient.Builder builder = RestClient.builder();
        builder.messageConverters(converters -> converters.add(textPlainJsonConverter));
        server = MockRestServiceServer.bindTo(builder).build();

        MarketProperties properties = new MarketProperties(BASE_URL, "stock", "shares", "TQBR", "LAST", "LOTSIZE");
        moexClient = new MoexClient(builder.build(), properties);
    }

    @DisplayName("getMarketSnapshot: returns price + lotSize from a single MOEX call")
    @Test
    void getMarketSnapshotReturnsPriceAndLotSize() {
        // Two boards; the lot size differs across boards (TQBT=1, TQBR=10) — the client
        // must pick the TQBR value, not the first non-null.
        String body = """
                {"marketdata": {
                  "columns": ["SECID","BOARDID","LAST"],
                  "data": [["SBER","TQBT",null], ["SBER","TQBR",312.45]]
                },
                "securities": {
                  "columns": ["SECID","BOARDID","LOTSIZE"],
                  "data": [["SBER","TQBT",1], ["SBER","TQBR",10]]
                }}
                """;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/SBER.json")))
                .andExpect(queryParam("iss.only", "marketdata,securities"))
                .andExpect(queryParam("iss.meta", "off"))
                .andExpect(queryParam("marketdata.columns", "SECID,BOARDID,LAST"))
                .andExpect(queryParam("securities.columns", "SECID,BOARDID,LOTSIZE"))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        MoexClient.MoexSnapshot snapshot = moexClient.getMarketSnapshot("SBER");

        assertThat(snapshot.last()).isEqualByComparingTo("312.45");
        assertThat(snapshot.lotSize()).isEqualTo(10); // TQBR, not TQBT (1)
        server.verify();
    }

    @DisplayName("getMarketSnapshot: picks the TQBR row when several boards carry different prices and lots")
    @Test
    void getMarketSnapshotPicksTqbrBoard() {
        // Three boards, each with its own price and lot size — only the TQBR row wins.
        String body = """
                {"marketdata": {
                  "columns": ["SECID","BOARDID","LAST"],
                  "data": [["GAZP","SMAL",98.02], ["GAZP","TQTF",50.00], ["GAZP","TQBR",96.84]]
                },
                "securities": {
                  "columns": ["SECID","BOARDID","LOTSIZE"],
                  "data": [["GAZP","SMAL",1], ["GAZP","TQTF",1], ["GAZP","TQBR",10]]
                }}
                """;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/GAZP.json")))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        MoexClient.MoexSnapshot snapshot = moexClient.getMarketSnapshot("GAZP");

        assertThat(snapshot.last()).isEqualByComparingTo("96.84"); // TQBR, not SMAL (98.02)
        assertThat(snapshot.lotSize()).isEqualTo(10);               // TQBR, not SMAL/TQTF (1)
    }

    @DisplayName("getMarketSnapshot: lotSize is null when the TQBR row is absent from securities (no fallback)")
    @Test
    void getMarketSnapshotLotSizeNullWhenTqbrAbsent() {
        // securities has only a non-TQBR board — no fallback to it, lotSize stays null.
        String body = """
                {"marketdata": {
                  "columns": ["SECID","BOARDID","LAST"],
                  "data": [["SBER","TQBR",312.45]]
                },
                "securities": {
                  "columns": ["SECID","BOARDID","LOTSIZE"],
                  "data": [["SBER","SMAL",1]]
                }}
                """;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/SBER.json")))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        MoexClient.MoexSnapshot snapshot = moexClient.getMarketSnapshot("SBER");

        assertThat(snapshot.last()).isEqualByComparingTo("312.45");
        assertThat(snapshot.lotSize()).isNull(); // no fallback to SMAL
    }

    @DisplayName("getMarketSnapshot: lotSize is null when MOEX omits the securities block")
    @Test
    void getMarketSnapshotLotSizeNullWhenNoSecurities() {
        String body = """
                {"marketdata": {
                  "columns": ["SECID","BOARDID","LAST"],
                  "data": [["SBER","TQBR",312.45]]
                }}
                """;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/SBER.json")))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        MoexClient.MoexSnapshot snapshot = moexClient.getMarketSnapshot("SBER");

        assertThat(snapshot.last()).isEqualByComparingTo("312.45");
        assertThat(snapshot.lotSize()).isNull();
    }

    @DisplayName("getCurrentPrice: returns LAST when MOEX returns rows with a price")
    @Test
    void getCurrentPriceReturnsLast() {
        String body = """
                {"marketdata": {
                  "columns": ["SECID","BOARDID","LAST"],
                  "data": [["SBER","TQBT",null], ["SBER","TQBR",312.45]]
                },
                "securities": {
                  "columns": ["SECID","BOARDID","LOTSIZE"],
                  "data": [["SBER","TQBR",10]]
                }}
                """;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/SBER.json")))
                .andExpect(queryParam("iss.only", "marketdata,securities"))
                .andExpect(queryParam("marketdata.columns", "SECID,BOARDID,LAST"))
                .andExpect(queryParam("securities.columns", "SECID,BOARDID,LOTSIZE"))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        BigDecimal price = moexClient.getCurrentPrice("SBER");

        assertThat(price).isEqualByComparingTo("312.45");
        server.verify();
    }

    @DisplayName("getMarketSnapshot: throws InvalidTickerException when MOEX returns no rows")
    @Test
    void getMarketSnapshotThrowsOnEmptyData() {
        String body = """
                {"marketdata": {
                  "columns": ["SECID","BOARDID","LAST"],
                  "data": []
                }}
                """;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/NOPE.json")))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> moexClient.getMarketSnapshot("NOPE"))
                .isInstanceOf(InvalidTickerException.class);
    }

    @DisplayName("getMarketSnapshot: throws MarketDataUnavailableException when LAST is null for every row")
    @Test
    void getMarketSnapshotThrowsWhenPriceMissing() {
        String body = """
                {"marketdata": {
                  "columns": ["SECID","BOARDID","LAST"],
                  "data": [["SBER","TQBT",null]]
                }}
                """;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/SBER.json")))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> moexClient.getMarketSnapshot("SBER"))
                .isInstanceOf(MarketDataUnavailableException.class);
    }

    @DisplayName("getMarketSnapshot: throws MarketDataUnavailableException on MOEX 5xx")
    @Test
    void getMarketSnapshotThrowsOnServerError() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/SBER.json")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> moexClient.getMarketSnapshot("SBER"))
                .isInstanceOf(MarketDataUnavailableException.class);
    }

    @DisplayName("getMarketSnapshot: throws InvalidTickerException for a blank ticker without calling MOEX")
    @Test
    void getMarketSnapshotThrowsOnBlankTicker() {
        assertThatThrownBy(() -> moexClient.getMarketSnapshot("  "))
                .isInstanceOf(InvalidTickerException.class);

        // No HTTP interaction should have been recorded.
        server.verify();
    }
}
