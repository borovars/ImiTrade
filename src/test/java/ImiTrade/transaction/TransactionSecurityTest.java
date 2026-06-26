package ImiTrade.transaction;

import ImiTrade.auth.dto.RegisterRequest;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
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

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests for {@code /api/v1/transactions}. Uses the real security filter chain
 * (no mocking of authentication) on a real PostgreSQL container, because the
 * {@code Transaction.type} column is a PostgreSQL {@code transaction_type} enum that H2
 * cannot persist. Mirrors the {@code TradeIntegrationTest} setup (Testcontainers +
 * Flyway {@code V1..V3} + JWT secret via {@link TestPropertySource}, no {@code test}
 * profile).
 *
 * <p>The JWT is minted by calling the real {@code /api/v1/auth/register} endpoint, then
 * a {@code Transaction} row is seeded for the resulting user so the authenticated case
 * returns 200 with content.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.jwt.secret-key=dGVzdC1zZWNyZXQta2V5LWZvci1iYWNrZW5kLXVuaXQtdGVzdHMtb25seS1uby1wcm9kdWN0aW9uLXVzZS1zdHJvbmctbGVuZ3RoLWtleQ==",
        "app.security.jwt.access-token-ttl=3600000",
        "app.security.jwt.issuer=imitrade"
})
class TransactionSecurityTest extends PostgresTestBase {

    private static final String EMAIL = "txuser@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private TransactionRepository transactionRepository;

    @Test
    @DisplayName("GET /api/v1/transactions without a JWT returns 401")
    void transactionsWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /api/v1/transactions with an invalid JWT returns 401")
    void transactionsWithInvalidTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private String registerAndExtractToken() throws Exception {
        RegisterRequest req = new RegisterRequest(EMAIL, "txuser", "S3cret!pass");
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
