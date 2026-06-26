package ImiTrade.account.web;

import ImiTrade.account.domain.AccountService;
import ImiTrade.account.dto.AccountResponse;
import ImiTrade.common.web.GlobalExceptionHandler;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice test for {@link AccountController}. The whole {@code ImiTrade.security}
 * package is excluded from component scanning and the security filter chain is skipped
 * (addFilters=false), so the web layer is tested in isolation. Security is covered
 * end-to-end by {@code AccountSecurityTest}.
 *
 * <p>Because the filter chain is off, {@code @AuthenticationPrincipal} would resolve to
 * {@code null} (nothing populates the {@code SecurityContextHolder}). The authenticated
 * principal is therefore installed directly into the holder before each test. MockMvc is
 * single-threaded, so the controller sees the same principal. The holder is cleared
 * after each test to avoid leaking state.
 */
@WebMvcTest(controllers = AccountController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "ImiTrade\\.security\\..*"))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    private static final long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthentication(authenticatedUser()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("GET /api/v1/account — 200 with the serialized account summary")
    @Test
    void getAccountReturnsSummary() throws Exception {
        AccountResponse summary = new AccountResponse(
                "arseny", "arseny@example.com",
                new BigDecimal("12500.5000"),
                new BigDecimal("4251.0000"),
                new BigDecimal("16751.5000"),
                new BigDecimal("146.0000"),
                2);
        given(accountService.getCurrentAccount(USER_ID)).willReturn(summary);

        mockMvc.perform(get("/api/v1/account").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("arseny"))
                .andExpect(jsonPath("$.email").value("arseny@example.com"))
                .andExpect(jsonPath("$.balance").value(12500.50))
                .andExpect(jsonPath("$.portfolioValue").value(4251.00))
                .andExpect(jsonPath("$.totalAssets").value(16751.50))
                .andExpect(jsonPath("$.profitLoss").value(146.00))
                .andExpect(jsonPath("$.positionsCount").value(2));
    }

    @DisplayName("GET /api/v1/account — 200 with zeroed aggregates for an empty portfolio")
    @Test
    void getAccountEmptyPortfolio() throws Exception {
        AccountResponse summary = new AccountResponse(
                "arseny", "arseny@example.com",
                new BigDecimal("12500.5000"),
                new BigDecimal("0.0000"),
                new BigDecimal("12500.5000"),
                new BigDecimal("0.0000"),
                0);
        given(accountService.getCurrentAccount(USER_ID)).willReturn(summary);

        mockMvc.perform(get("/api/v1/account").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioValue").value(0.00))
                .andExpect(jsonPath("$.profitLoss").value(0.00))
                .andExpect(jsonPath("$.totalAssets").value(12500.50))
                .andExpect(jsonPath("$.positionsCount").value(0));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(USER_ID, "arseny@example.com");
    }
}
