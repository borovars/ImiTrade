package ImiTrade.account;

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
 * Security tests for {@code /api/v1/account}. Uses the real security filter chain
 * (no mocking of authentication) on an in-memory H2 database. The JWT is minted by
 * calling the real {@code /api/v1/auth/register} endpoint, mirroring
 * {@code PortfolioSecurityTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountSecurityTest {

    private static final String EMAIL = "account@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StockRepository stockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PortfolioPositionRepository portfolioPositionRepository;

    @Test
    @DisplayName("GET /api/v1/account without a JWT returns 401")
    void accountWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/account"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /api/v1/account with an invalid JWT returns 401")
    void accountWithInvalidTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/account")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /api/v1/account with a valid JWT and no holdings returns 200 with zeroed aggregates")
    void accountWithValidTokenEmptyIs200() throws Exception {
        String token = registerAndExtractToken();

        // a freshly registered user has no positions: portfolioValue/profitLoss = 0,
        // totalAssets = balance (initial), positionsCount = 0
        mockMvc.perform(get("/api/v1/account")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("accountuser"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.balance").value(500000.00))
                .andExpect(jsonPath("$.portfolioValue").value(0.00))
                .andExpect(jsonPath("$.totalAssets").value(500000.00))
                .andExpect(jsonPath("$.profitLoss").value(0.00))
                .andExpect(jsonPath("$.positionsCount").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/account with a valid JWT and a seeded position returns 200 with the computed aggregates")
    void accountWithValidTokenAndPositionIs200() throws Exception {
        String token = registerAndExtractToken();
        Long userId = userRepository.findByEmail(EMAIL).map(User::getId).orElseThrow();

        Stock sber = stockRepository.save(Stock.builder()
                .ticker("SBER").companyName("Сбербанк").exchange("MOEX")
                .currentPrice(new BigDecimal("215.1000")).build());
        portfolioPositionRepository.save(PortfolioPosition.builder()
                .userId(userId).stockId(sber.getId())
                .quantity(10).averagePrice(new BigDecimal("210.5000")).build());

        // portfolioValue = 215.1000*10 = 2151.0000
        // profitLoss     = (215.1000-210.5000)*10 = 46.0000
        // totalAssets    = 500000.0000 + 2151.0000 = 502151.0000
        mockMvc.perform(get("/api/v1/account")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioValue").value(2151.00))
                .andExpect(jsonPath("$.profitLoss").value(46.00))
                .andExpect(jsonPath("$.totalAssets").value(502151.00))
                .andExpect(jsonPath("$.positionsCount").value(1));
    }

    private String registerAndExtractToken() throws Exception {
        RegisterRequest req = new RegisterRequest(EMAIL, "accountuser", "S3cret!pass");
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
