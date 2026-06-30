package ImiTrade.portfolio.web;

import ImiTrade.common.web.GlobalExceptionHandler;
import ImiTrade.portfolio.domain.PortfolioService;
import ImiTrade.portfolio.dto.PortfolioResponse;
import ImiTrade.security.AuthenticatedUser;
import ImiTrade.security.JwtAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice test for {@link PortfolioController}. The whole
 * {@code ImiTrade.security} package is excluded from component scanning and the
 * security filter chain is skipped (addFilters=false), so the web layer is tested in
 * isolation. Security is covered end-to-end by {@code PortfolioSecurityTest}.
 *
 * <p>Because the filter chain is off, {@code @AuthenticationPrincipal} would resolve to
 * {@code null} (nothing populates the {@code SecurityContextHolder}). The authenticated
 * principal is therefore installed directly into the holder before each test. MockMvc is
 * single-threaded, so the controller sees the same principal. The holder is cleared
 * after each test to avoid leaking state.
 */
@WebMvcTest(controllers = PortfolioController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "ImiTrade\\.security\\..*"))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class PortfolioControllerTest {

    private static final long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PortfolioService portfolioService;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthentication(authenticatedUser()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("GET /api/v1/portfolio — 200 with the serialized portfolio list")
    @Test
    void getPortfolioReturnsList() throws Exception {
        PortfolioResponse line = new PortfolioResponse(
                1L, "SBER", "Сбербанк", 10,
                new BigDecimal("210.5000"), new BigDecimal("215.1000"), new BigDecimal("46.0000"));
        given(portfolioService.getPortfolio(USER_ID)).willReturn(List.of(line));

        mockMvc.perform(get("/api/v1/portfolio")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].stockId").value(1))
                .andExpect(jsonPath("$[0].ticker").value("SBER"))
                .andExpect(jsonPath("$[0].companyName").value("Сбербанк"))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].averagePrice").value(210.50))
                .andExpect(jsonPath("$[0].currentPrice").value(215.10))
                .andExpect(jsonPath("$[0].pnl").value(46.00));
    }

    @DisplayName("GET /api/v1/portfolio — 200 with an empty array when there are no holdings")
    @Test
    void getPortfolioEmpty() throws Exception {
        given(portfolioService.getPortfolio(USER_ID)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/portfolio")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(USER_ID, "portfolio@example.com");
    }
}
