package ImiTrade.stocks.web;

import ImiTrade.common.exception.StockNotFoundException;
import ImiTrade.common.web.GlobalExceptionHandler;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice test for {@link StockController}. The whole {@code ImiTrade.security}
 * package is excluded from component scanning and the security filter chain is skipped
 * (addFilters=false), so the web layer is tested in isolation. Security is covered
 * end-to-end by {@code StockSecurityTest}.
 */
@WebMvcTest(controllers = StockController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "ImiTrade\\.security\\..*"))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockService stockService;

    @DisplayName("GET /api/v1/stocks — 200 with serialized stock page")
    @Test
    void getStocksReturnsPage() throws Exception {
        Stock sber = Stock.builder().id(1L).ticker("SBER").companyName("Сбербанк").exchange("MOEX").build();
        Stock gazp = Stock.builder().id(2L).ticker("GAZP").companyName("Газпром").exchange("MOEX").build();
        given(stockService.getStocks(eq(null), eq(null), any()))
                .willReturn(new PageImpl<>(List.of(sber, gazp), PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/v1/stocks")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].ticker").value("SBER"))
                .andExpect(jsonPath("$.content[0].companyName").value("Сбербанк"))
                .andExpect(jsonPath("$.content[0].exchange").value("MOEX"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @DisplayName("GET /api/v1/stocks/{id} — 200 with serialized stock for an existing id")
    @Test
    void getStockByIdFound() throws Exception {
        Stock sber = Stock.builder().id(1L).ticker("SBER").companyName("Сбербанк").exchange("MOEX").build();
        given(stockService.getStockById(1L)).willReturn(sber);

        mockMvc.perform(get("/api/v1/stocks/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ticker").value("SBER"))
                .andExpect(jsonPath("$.companyName").value("Сбербанк"))
                .andExpect(jsonPath("$.exchange").value("MOEX"));
    }

    @DisplayName("GET /api/v1/stocks/{id} — 404 with STOCK_NOT_FOUND code for a missing stock")
    @Test
    void getStockByIdMissing() throws Exception {
        given(stockService.getStockById(99L)).willThrow(new StockNotFoundException(99L));

        mockMvc.perform(get("/api/v1/stocks/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STOCK_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Stock not found"));
    }
}
