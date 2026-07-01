package ImiTrade.market;

import ImiTrade.auth.dto.RegisterRequest;
import ImiTrade.market.domain.MarketDataScheduler;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import ImiTrade.testsupport.PostgresTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for {@link MarketDataScheduler} on a real PostgreSQL
 * container (Flyway applies the real {@code V1..V3} migrations, so SBER is seeded with
 * {@code current_price 310.5000}). The MOEX integration is mocked, so no real HTTP
 * request is made.
 *
 * <p>JWT secret is supplied via {@link TestPropertySource} so the application context
 * boots without the {@code APP_JWT_SECRET} environment variable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.jwt.secret-key=dGVzdC1zZWNyZXQta2V5LWZvci1iYWNrZW5kLXVuaXQtdGVzdHMtb25seS1uby1wcm9kdWN0aW9uLXVzZS1zdHJvbmctbGVuZ3RoLWtleQ==",
        "app.security.jwt.access-token-ttl=3600000",
        "app.security.jwt.issuer=imitrade",
        // keep the bean wired so we can call refreshPrices() directly, but push the
        // auto-fire period far out so no background run interferes with the test.
        "app.market.scheduler.enabled=true",
        "app.market.scheduler.fixed-rate=999999999"
})
class MarketDataSchedulerIntegrationTest extends PostgresTestBase {

    private static final String SBER_TICKER = "SBER";
    private static final BigDecimal SEEDED_SBER_PRICE = new BigDecimal("310.5000");
    private static final BigDecimal REFRESHED_SBER_PRICE = new BigDecimal("999.0000");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MarketDataScheduler scheduler;
    @Autowired
    private StockRepository stockRepository;

    /** Replaces the real MOEX-backed service so no live HTTP request is made. */
    @MockitoBean
    private ImiTrade.market.domain.MarketDataService marketDataService;

    /**
     * The scheduler commits its writes (each ticker in its own transaction), so changes
     * leak between tests sharing this container. Reset SBER to its seeded price so every
     * test starts from a deterministic baseline.
     */
    @BeforeEach
    void resetSberPrice() {
        stockRepository.updateCurrentPrice(sber().getId(), SEEDED_SBER_PRICE);
    }

    @Test
    @DisplayName("refreshPrices persists the new current_price in the database")
    void refreshUpdatesPriceInDb() {
        Stock sber = sber();
        assertThat(sber.getCurrentPrice()).isEqualByComparingTo(SEEDED_SBER_PRICE);

        when(marketDataService.getCurrentPrice(eq(SBER_TICKER))).thenReturn(REFRESHED_SBER_PRICE);
        scheduler.refreshPrices();

        BigDecimal persisted = stockRepository.findById(sber.getId()).orElseThrow().getCurrentPrice();
        assertThat(persisted).isEqualByComparingTo(REFRESHED_SBER_PRICE);
    }

    @Test
    @DisplayName("Trading uses the refreshed current_price with no change to TradeService")
    void tradingUsesRefreshedPrice() throws Exception {
        // 1) refresh SBER to the new price
        when(marketDataService.getCurrentPrice(eq(SBER_TICKER))).thenReturn(REFRESHED_SBER_PRICE);
        scheduler.refreshPrices();

        // 2) a buy trade must price at the refreshed value (1 share @ 999.0000 = 999.0000)
        String token = registerAndExtractToken(
                new RegisterRequest("trader@example.com", "trader", "S3cret!pass", null));

        MvcResult result = mockMvc.perform(post("/api/v1/trades/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BuyReq(sber().getId(), 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockTicker").value(SBER_TICKER))
                .andExpect(jsonPath("$.price").value(REFRESHED_SBER_PRICE.doubleValue()))
                .andExpect(jsonPath("$.totalAmount").value(REFRESHED_SBER_PRICE.doubleValue()))
                .andReturn();

        // TradeService is unchanged and has no MOEX dependency; it priced the order from the DB
        Long transactionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("transactionId").asLong();
        Optional<Stock> sberAfter = stockRepository.findById(sber().getId());
        assertThat(sberAfter).isPresent();
        assertThat(sberAfter.orElseThrow().getCurrentPrice()).isEqualByComparingTo(REFRESHED_SBER_PRICE);
        assertThat(transactionId).isNotNull();
    }

    // ---- helpers ----

    private Stock sber() {
        return stockRepository.findAll().stream()
                .filter(s -> SBER_TICKER.equals(s.getTicker()))
                .findFirst().orElseThrow();
    }

    private String registerAndExtractToken(RegisterRequest req) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private record BuyReq(Long stockId, Integer quantity) {
    }
}
