package ImiTrade.portfolio;

import ImiTrade.auth.dto.RegisterRequest;
import ImiTrade.portfolio.domain.PortfolioPosition;
import ImiTrade.portfolio.domain.PortfolioPositionRepository;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserRepository;
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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration test for {@code GET /api/v1/portfolio}. The user's
 * portfolio position is seeded directly via the repository: it reflects the same
 * shape a {@code BUY} trade leaves behind in {@code portfolio_positions} (the trade
 * path itself is covered by {@code TradeIntegrationTest} on PostgreSQL Testcontainers,
 * since the {@code transaction_type} enum is not supported by H2). The endpoint under
 * test only reads {@code portfolio_positions} + {@code stocks}, which H2 handles fine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PortfolioIntegrationTest {

    private static final String EMAIL = "portfolio-int@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private PortfolioPositionRepository portfolioPositionRepository;

    @Test
    @DisplayName("a registered user with a position sees it with the correct pnl via GET /api/v1/portfolio")
    void portfolioReturnsPositionWithPnl() throws Exception {
        String token = registerAndExtractToken();
        Long userId = userRepository.findByEmail(EMAIL).map(User::getId).orElseThrow();

        Stock aapl = stockRepository.save(Stock.builder()
                .ticker("AAPL").companyName("Apple Inc.").exchange("NASDAQ")
                .currentPrice(new BigDecimal("215.1000")).build());
        portfolioPositionRepository.save(PortfolioPosition.builder()
                .userId(userId).stockId(aapl.getId())
                .quantity(10).averagePrice(new BigDecimal("210.5000")).build());

        mockMvc.perform(get("/api/v1/portfolio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockId").value(aapl.getId().intValue()))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].companyName").value("Apple Inc."))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].averagePrice").value(210.50))
                .andExpect(jsonPath("$[0].currentPrice").value(215.10))
                .andExpect(jsonPath("$[0].pnl").value(46.00));
    }

    @Test
    @DisplayName("the portfolio reflects a price move: pnl recomputes against the live stock price")
    void portfolioRecomputesPnlOnPriceChange() throws Exception {
        String token = registerAndExtractToken();
        Long userId = userRepository.findByEmail(EMAIL).map(User::getId).orElseThrow();

        Stock aapl = stockRepository.save(Stock.builder()
                .ticker("AAPL").companyName("Apple Inc.").exchange("NASDAQ")
                .currentPrice(new BigDecimal("215.1000")).build());
        portfolioPositionRepository.save(PortfolioPosition.builder()
                .userId(userId).stockId(aapl.getId())
                .quantity(10).averagePrice(new BigDecimal("210.5000")).build());

        // Simulate a market price move after the position was opened.
        aapl.setCurrentPrice(new BigDecimal("200.0000"));
        stockRepository.save(aapl);

        mockMvc.perform(get("/api/v1/portfolio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // (200.0 - 210.5) * 10 = -105.0000
                .andExpect(jsonPath("$[0].pnl").value(-105.00));
    }

    @Test
    @DisplayName("a user with no holdings gets an empty portfolio")
    void portfolioEmptyForNewUser() throws Exception {
        String token = registerAndExtractToken();

        mockMvc.perform(get("/api/v1/portfolio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    private String registerAndExtractToken() throws Exception {
        RegisterRequest req = new RegisterRequest(EMAIL, "portfolioint", "S3cret!pass");
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
