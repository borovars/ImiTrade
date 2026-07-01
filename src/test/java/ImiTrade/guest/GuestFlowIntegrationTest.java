package ImiTrade.guest;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full guest-to-registered flow integration test on a real PostgreSQL container.
 * Flyway applies V1..V4, so the {@code transaction_type} enum and seeded stocks are available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.jwt.secret-key=dGVzdC1zZWNyZXQta2V5LWZvci1iYWNrZW5kLXVuaXQtdGVzdHMtb25seS1uby1wcm9kdWN0aW9uLXVzZS1zdHJvbmctbGVuZ3RoLWtleQ==",
        "app.security.jwt.access-token-ttl=3600000",
        "app.security.jwt.issuer=imitrade",
        "app.market.scheduler.enabled=false"
})
class GuestFlowIntegrationTest extends PostgresTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PortfolioPositionRepository portfolioPositionRepository;
    @Autowired private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Full guest flow: create guest → trade → register → portfolio, balance and history preserved")
    void fullGuestFlow() throws Exception {
        // 1. Create guest
        String guestResponse = mockMvc.perform(post("/api/v1/guest"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String guestToken = objectMapper.readTree(guestResponse).get("guestToken").asText();
        Long guestUserId = userRepository.findByGuestToken(java.util.UUID.fromString(guestToken)).orElseThrow().getId();

        // 2. Account summary as guest (balance = 100000)
        mockMvc.perform(get("/api/v1/account")
                        .header("X-Guest-Token", guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100000.0))
                .andExpect(jsonPath("$.positionsCount").value(0));

        // 3. Buy 10 shares of SBER (id=1, current_price=310.5000 from V3)
        MvcResult buyResult = mockMvc.perform(post("/api/v1/trades/buy")
                        .header("X-Guest-Token", guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BuyReq(1L, 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockTicker").value("SBER"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.totalAmount").value(3105.0))
                .andReturn();
        Long transactionId = objectMapper.readTree(buyResult.getResponse().getContentAsString())
                .get("transactionId").asLong();

        // 4. Portfolio as guest
        mockMvc.perform(get("/api/v1/portfolio")
                        .header("X-Guest-Token", guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("SBER"))
                .andExpect(jsonPath("$[0].quantity").value(10));

        // 5. Transactions as guest
        mockMvc.perform(get("/api/v1/transactions")
                        .header("X-Guest-Token", guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("BUY"))
                .andExpect(jsonPath("$.content[0].quantity").value(10));

        // 6. Register with guestToken
        RegisterRequest registerReq = new RegisterRequest(
                "alice@example.com", "alice", "S3cret!pass", guestToken);
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        String jwt = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("token").asText();

        // 7. Verify user is now registered with bonus (100000 - 3105 + 400000 = 496895)
        User registered = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(registered.getIsGuest()).isFalse();
        assertThat(registered.getGuestToken()).isNull();
        assertThat(registered.getBalance()).isEqualByComparingTo(new BigDecimal("496895.0000"));
        assertThat(registered.getId()).isEqualTo(guestUserId); // same user

        // 8. Account summary with JWT still shows the portfolio and correct balance
        mockMvc.perform(get("/api/v1/account")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(496895.0))
                .andExpect(jsonPath("$.positionsCount").value(1));

        // 9. Portfolio with JWT preserved
        mockMvc.perform(get("/api/v1/portfolio")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("SBER"))
                .andExpect(jsonPath("$[0].quantity").value(10));

        // 10. Transactions with JWT preserved
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("BUY"))
                .andExpect(jsonPath("$.content[0].quantity").value(10));

        // 11. Verify the same transaction still exists in DB
        Transaction tx = transactionRepository.findById(transactionId).orElseThrow();
        assertThat(tx.getUserId()).isEqualTo(guestUserId);
        assertThat(tx.getType()).isEqualTo(TransactionType.BUY);

        // 12. Verify portfolio position still exists in DB
        List<PortfolioPosition> positions = portfolioPositionRepository.findByUserId(guestUserId);
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).getQuantity()).isEqualTo(10);
    }

    private record BuyReq(Long stockId, Integer quantity) {
    }
}
