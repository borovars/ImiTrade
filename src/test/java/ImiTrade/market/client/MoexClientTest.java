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

        MarketProperties properties = new MarketProperties(BASE_URL, "stock", "shares", "LAST");
        moexClient = new MoexClient(builder.build(), properties);
    }

    @DisplayName("getCurrentPrice: returns LAST when MOEX returns rows with a price")
    @Test
    void getCurrentPriceReturnsLast() {
        // Several boards; some without a trade yet — the client picks the first non-null price.
        String body = """
                {"marketdata": {
                  "columns": ["SECID","BOARDID","LAST"],
                  "data": [["SBER","TQBT",null], ["SBER","TQBR",312.45]]
                }}
                """;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/SBER.json")))
                .andExpect(queryParam("iss.only", "marketdata"))
                .andExpect(queryParam("iss.meta", "off"))
                .andExpect(queryParam("marketdata.columns", "SECID,BOARDID,LAST"))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        BigDecimal price = moexClient.getCurrentPrice("SBER");

        assertThat(price).isEqualByComparingTo("312.45");
        server.verify();
    }

    @DisplayName("getCurrentPrice: throws InvalidTickerException when MOEX returns no rows")
    @Test
    void getCurrentPriceThrowsOnEmptyData() {
        String body = """
                {"marketdata": {
                  "columns": ["SECID","BOARDID","LAST"],
                  "data": []
                }}
                """;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/NOPE.json")))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> moexClient.getCurrentPrice("NOPE"))
                .isInstanceOf(InvalidTickerException.class);
    }

    @DisplayName("getCurrentPrice: throws MarketDataUnavailableException when LAST is null for every row")
    @Test
    void getCurrentPriceThrowsWhenPriceMissing() {
        String body = """
                {"marketdata": {
                  "columns": ["SECID","BOARDID","LAST"],
                  "data": [["SBER","TQBT",null]]
                }}
                """;
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/SBER.json")))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> moexClient.getCurrentPrice("SBER"))
                .isInstanceOf(MarketDataUnavailableException.class);
    }

    @DisplayName("getCurrentPrice: throws MarketDataUnavailableException on MOEX 5xx")
    @Test
    void getCurrentPriceThrowsOnServerError() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/engines/stock/markets/shares/securities/SBER.json")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> moexClient.getCurrentPrice("SBER"))
                .isInstanceOf(MarketDataUnavailableException.class);
    }

    @DisplayName("getCurrentPrice: throws InvalidTickerException for a blank ticker without calling MOEX")
    @Test
    void getCurrentPriceThrowsOnBlankTicker() {
        assertThatThrownBy(() -> moexClient.getCurrentPrice("  "))
                .isInstanceOf(InvalidTickerException.class);

        // No HTTP interaction should have been recorded.
        server.verify();
    }
}
