package ImiTrade.stocks;

import ImiTrade.auth.dto.RegisterRequest;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests for {@code /api/v1/stocks/**}. Uses the real security filter chain
 * (no mocking of authentication) on an in-memory H2 database. Stocks are seeded per
 * test so the authenticated case returns 200 with content.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StockSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository.save(Stock.builder()
                .ticker("AAPL").companyName("Apple Inc.").exchange("NASDAQ")
                .currentPrice(new BigDecimal("212.3500")).build());
    }

    @Test
    @DisplayName("GET /api/v1/stocks without a JWT returns 401")
    void stocksWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/stocks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /api/v1/stocks/{id} without a JWT returns 401")
    void stockByIdWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /api/v1/stocks with a valid JWT returns 200")
    void stocksWithValidTokenIs200() throws Exception {
        String token = registerAndExtractToken();

        mockMvc.perform(get("/api/v1/stocks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticker").value("AAPL"));
    }

    private String registerAndExtractToken() throws Exception {
        RegisterRequest req = new RegisterRequest("stocks@example.com", "stocksuser", "S3cret!pass");
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
