package ImiTrade.transaction.web;

import ImiTrade.common.web.GlobalExceptionHandler;
import ImiTrade.security.AuthenticatedUser;
import ImiTrade.security.JwtAuthentication;
import ImiTrade.transaction.domain.TransactionService;
import ImiTrade.transaction.domain.TransactionType;
import ImiTrade.transaction.dto.TransactionResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice test for {@link TransactionController}. The whole
 * {@code ImiTrade.security} package is excluded from component scanning and the
 * security filter chain is skipped (addFilters=false), so the web layer is tested in
 * isolation. Security is covered end-to-end by {@code TransactionSecurityTest}.
 *
 * <p>Because the filter chain is off, {@code @AuthenticationPrincipal} would resolve to
 * {@code null} (nothing populates the {@code SecurityContextHolder}). The authenticated
 * principal is therefore installed directly into the holder before each test. MockMvc is
 * single-threaded, so the controller sees the same principal. The holder is cleared
 * after each test to avoid leaking state.
 */
@WebMvcTest(controllers = TransactionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "ImiTrade\\.security\\..*"))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    private static final long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthentication(authenticatedUser()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("GET /api/v1/transactions — 200 with the serialized page")
    @Test
    void getTransactionsReturnsPage() throws Exception {
        TransactionResponse line = new TransactionResponse(
                15L, 1L, "AAPL", "BUY", 10,
                new BigDecimal("210.5000"), new BigDecimal("2105.0000"),
                Instant.parse("2026-06-25T10:15:00Z"));
        Page<TransactionResponse> page = new PageImpl<>(List.of(line), PageRequest.of(0, 20), 1);
        given(transactionService.getTransactions(eq(USER_ID), any(), any(), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/transactions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].id").value(15))
                .andExpect(jsonPath("$.content[0].stockId").value(1))
                .andExpect(jsonPath("$.content[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.content[0].type").value("BUY"))
                .andExpect(jsonPath("$.content[0].quantity").value(10))
                .andExpect(jsonPath("$.content[0].price").value(210.50))
                .andExpect(jsonPath("$.content[0].totalAmount").value(2105.00));
    }

    @DisplayName("GET /api/v1/transactions — 200 with an empty page when there is no history")
    @Test
    void getTransactionsEmpty() throws Exception {
        Page<TransactionResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        given(transactionService.getTransactions(eq(USER_ID), any(), any(), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/transactions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @DisplayName("GET /api/v1/transactions?type=SELL — type filter is forwarded to the service")
    @Test
    void getTransactionsWithTypeFilter() throws Exception {
        TransactionResponse line = new TransactionResponse(
                16L, 1L, "AAPL", "SELL", 3,
                new BigDecimal("215.0000"), new BigDecimal("645.0000"),
                Instant.parse("2026-06-25T11:00:00Z"));
        Page<TransactionResponse> page = new PageImpl<>(List.of(line), PageRequest.of(0, 20), 1);
        given(transactionService.getTransactions(eq(USER_ID), eq(TransactionType.SELL), any(), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/transactions")
                        .param("type", "SELL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("SELL"))
                .andExpect(jsonPath("$.content[0].quantity").value(3));
    }

    @DisplayName("GET /api/v1/transactions?stockId=1 — stockId filter is forwarded to the service")
    @Test
    void getTransactionsWithStockIdFilter() throws Exception {
        Page<TransactionResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        given(transactionService.getTransactions(eq(USER_ID), any(), eq(1L), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/transactions")
                        .param("stockId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(0)));
    }

    @DisplayName("GET /api/v1/transactions?page=0&size=2 — paging params are forwarded to the service")
    @Test
    void getTransactionsWithPaging() throws Exception {
        TransactionResponse first = new TransactionResponse(
                1L, 1L, "AAPL", "BUY", 10,
                new BigDecimal("210.5000"), new BigDecimal("2105.0000"),
                Instant.parse("2026-06-25T10:15:00Z"));
        Page<TransactionResponse> page = new PageImpl<>(List.of(first), PageRequest.of(0, 2), 5);
        given(transactionService.getTransactions(eq(USER_ID), isNull(), isNull(), any()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/transactions")
                        .param("page", "0")
                        .param("size", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(USER_ID, "tx@example.com");
    }
}
