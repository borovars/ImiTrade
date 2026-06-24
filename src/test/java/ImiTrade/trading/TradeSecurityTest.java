package ImiTrade.trading;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests for {@code /api/v1/trades/**} on the in-memory H2 database.
 *
 * <p>Only verifies that unauthenticated requests receive 401. Authenticated-trade
 * tests that exercise the PostgreSQL {@code transaction_type} enum live in
 * {@link ImiTrade.trading.TradeIntegrationTest} (Testcontainers).
 *
 * <p>H2 does not support PostgreSQL enum types, so Hibernate's
 * {@code @JdbcTypeCode(SqlTypes.NAMED_ENUM)} on the {@code Transaction.type}
 * field would cause a {@code ClassCastException} when trying to persist a trade
 * row on H2. Therefore, no real buy/sell is attempted here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TradeSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/trades/buy without a JWT returns 401")
    void buyWithoutTokenIs401() throws Exception {
        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockId\":1,\"quantity\":10}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("POST /api/v1/trades/sell without a JWT returns 401")
    void sellWithoutTokenIs401() throws Exception {
        mockMvc.perform(post("/api/v1/trades/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockId\":1,\"quantity\":5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
