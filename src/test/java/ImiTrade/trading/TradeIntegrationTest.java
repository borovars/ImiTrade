package ImiTrade.trading;

import ImiTrade.auth.dto.RegisterRequest;
import ImiTrade.portfolio.domain.PortfolioPosition;
import ImiTrade.portfolio.domain.PortfolioPositionRepository;
import ImiTrade.testsupport.PostgresTestBase;
import ImiTrade.transaction.domain.Transaction;
import ImiTrade.transaction.domain.TransactionRepository;
import ImiTrade.transaction.domain.TransactionType;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for the trading endpoints on a real PostgreSQL
 * container (Flyway applies the real {@code V1..V3} migrations, including the
 * {@code transaction_type} enum). No H2 and no {@code test} profile.
 *
 * <p>JWT secret is supplied via {@link TestPropertySource} so the application
 * context boots without the {@code APP_JWT_SECRET} environment variable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.jwt.secret-key=dGVzdC1zZWNyZXQta2V5LWZvci1iYWNrZW5kLXVuaXQtdGVzdHMtb25seS1uby1wcm9kdWN0aW9uLXVzZS1zdHJvbmctbGVuZ3RoLWtleQ==",
        "app.security.jwt.access-token-ttl=3600000",
        "app.security.jwt.issuer=imitrade",
        "app.market.scheduler.enabled=false"
})
class TradeIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private PortfolioPositionRepository portfolioPositionRepository;

    @Test
    @DisplayName("POST /api/v1/trades/buy — 200, creates transaction, debits balance, creates position")
    void buyTrade() throws Exception {
        RegisterRequest register = new RegisterRequest("trader@example.com", "trader", "S3cret!pass", null);
        String token = registerAndExtractToken(register);
        Long userId = userRepository.findByEmail("trader@example.com").orElseThrow().getId();
        BigDecimal balanceBefore = userRepository.findById(userId).orElseThrow().getBalance();

        // SBER is seeded by V2 with current_price 310.5000 (V3); id = 1
        BuyReq req = new BuyReq(1L, 10);
        MvcResult result = mockMvc.perform(post("/api/v1/trades/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").isNumber())
                .andExpect(jsonPath("$.stockTicker").value("SBER"))
                .andExpect(jsonPath("$.type").value("BUY"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.price").value(310.5000))
                .andExpect(jsonPath("$.totalAmount").value(3105.0000))
                .andReturn();

        Long transactionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("transactionId").asLong();

        // a BUY transaction row exists
        Transaction tx = transactionRepository.findById(transactionId).orElseThrow();
        assertThat(tx.getType()).isEqualTo(TransactionType.BUY);
        assertThat(tx.getUserId()).isEqualTo(userId);
        assertThat(tx.getStockId()).isEqualTo(1L);
        assertThat(tx.getTotalAmount()).isEqualByComparingTo(new BigDecimal("3105.0000"));

        // balance decreased by the trade total
        BigDecimal balanceAfter = userRepository.findById(userId).orElseThrow().getBalance();
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.subtract(new BigDecimal("3105.0000")));

        // a portfolio position was created
        PortfolioPosition position = portfolioPositionRepository
                .findByUserIdAndStockId(userId, 1L).orElseThrow();
        assertThat(position.getQuantity()).isEqualTo(10);
        assertThat(position.getAveragePrice()).isEqualByComparingTo(new BigDecimal("310.5000"));
    }

    @Test
    @DisplayName("POST /api/v1/trades/sell — 200, creates transaction, credits balance, decreases quantity")
    void sellTrade() throws Exception {
        RegisterRequest register = new RegisterRequest("seller@example.com", "seller", "S3cret!pass", null);
        String token = registerAndExtractToken(register);
        Long userId = userRepository.findByEmail("seller@example.com").orElseThrow().getId();

        // precondition: buy 10 shares first
        mockMvc.perform(post("/api/v1/trades/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BuyReq(1L, 10))))
                .andExpect(status().isOk());

        BigDecimal balanceBefore = userRepository.findById(userId).orElseThrow().getBalance();

        // sell 4 of 10 shares of SBER at 310.5000 -> total 1242.0000
        SellReq req = new SellReq(1L, 4);
        MvcResult result = mockMvc.perform(post("/api/v1/trades/sell")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SELL"))
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.totalAmount").value(1242.0000))
                .andReturn();

        Long transactionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("transactionId").asLong();

        Transaction tx = transactionRepository.findById(transactionId).orElseThrow();
        assertThat(tx.getType()).isEqualTo(TransactionType.SELL);

        // balance increased by the trade total
        BigDecimal balanceAfter = userRepository.findById(userId).orElseThrow().getBalance();
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.add(new BigDecimal("1242.0000")));

        // position quantity decreased to 6
        PortfolioPosition position = portfolioPositionRepository
                .findByUserIdAndStockId(userId, 1L).orElseThrow();
        assertThat(position.getQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("POST /api/v1/trades/buy with unknown stock returns 404")
    void buyUnknownStock() throws Exception {
        String token = registerAndExtractToken(
                new RegisterRequest("noStock@example.com", "nostock", "S3cret!pass", null));

        mockMvc.perform(post("/api/v1/trades/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BuyReq(999L, 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STOCK_NOT_FOUND"));
    }

    // ---- helpers ----

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

    private record SellReq(Long stockId, Integer quantity) {
    }
}
