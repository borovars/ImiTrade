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
 * Security tests for {@code /api/v1/portfolio}. Uses the real security filter chain
 * (no mocking of authentication) on an in-memory H2 database. The JWT is minted by
 * calling the real {@code /api/v1/auth/register} endpoint, mirroring
 * {@code StockSecurityTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PortfolioSecurityTest {

    private static final String EMAIL = "portfolio@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StockRepository stockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PortfolioPositionRepository portfolioPositionRepository;

    @Test
    @DisplayName("GET /api/v1/portfolio without a JWT returns 401")
    void portfolioWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /api/v1/portfolio with an invalid JWT returns 401")
    void portfolioWithInvalidTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /api/v1/portfolio with a valid JWT and no holdings returns 200 with an empty array")
    void portfolioWithValidTokenEmptyIs200() throws Exception {
        String token = registerAndExtractToken();

        mockMvc.perform(get("/api/v1/portfolio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/portfolio with a valid JWT and a seeded position returns 200 with the line and computed pnl")
    void portfolioWithValidTokenAndPositionIs200() throws Exception {
        String token = registerAndExtractToken();
        Long userId = userRepository.findByEmail(EMAIL).map(User::getId).orElseThrow();

        Stock sber = stockRepository.save(Stock.builder()
                .ticker("SBER").companyName("Сбербанк").exchange("MOEX")
                .currentPrice(new BigDecimal("310.5000")).lotSize(1).build());
        portfolioPositionRepository.save(PortfolioPosition.builder()
                .userId(userId).stockId(sber.getId())
                .quantity(10).averagePrice(new BigDecimal("305.9000")).build());

        mockMvc.perform(get("/api/v1/portfolio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("SBER"))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].pnl").value(46.00));
    }

    private String registerAndExtractToken() throws Exception {
        RegisterRequest req = new RegisterRequest(EMAIL, "portfoliouser", "S3cret!pass", null);
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
