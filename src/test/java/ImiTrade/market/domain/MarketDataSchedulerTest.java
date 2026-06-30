package ImiTrade.market.domain;

import ImiTrade.common.exception.MarketDataUnavailableException;
import ImiTrade.market.config.SchedulerProperties;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MarketDataScheduler} with Mockito. The persistence layer and
 * {@link MarketDataService} are mocked; no Spring context, no DB, no MOEX calls.
 */
@ExtendWith(MockitoExtension.class)
class MarketDataSchedulerTest {

    @Mock
    private MarketDataService marketDataService;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private SchedulerProperties schedulerProperties;

    @InjectMocks
    private MarketDataScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(schedulerProperties.fixedRate()).thenReturn(60000L);
    }

    @DisplayName("refreshPrices: updates every stock — getCurrentPrice + updateCurrentPrice called for each")
    @Test
    void refreshAllStocks() {
        Stock sber = stock(1L, "SBER", new BigDecimal("290.5000"));
        Stock gazp = stock(2L, "GAZP", new BigDecimal("170.2000"));
        when(stockRepository.findAll()).thenReturn(List.of(sber, gazp));
        when(marketDataService.getCurrentPrice("SBER")).thenReturn(new BigDecimal("300.0000"));
        when(marketDataService.getCurrentPrice("GAZP")).thenReturn(new BigDecimal("180.0000"));

        scheduler.refreshPrices();

        verify(marketDataService, times(1)).getCurrentPrice("SBER");
        verify(marketDataService, times(1)).getCurrentPrice("GAZP");
        verify(stockRepository).updateCurrentPrice(eq(1L), eq(new BigDecimal("300.0000")));
        verify(stockRepository).updateCurrentPrice(eq(2L), eq(new BigDecimal("180.0000")));
    }

    @DisplayName("refreshPrices: persists the freshly fetched price (not the stale one) for each ticker")
    @Test
    void updatesWithFreshPrice() {
        Stock lkoh = stock(3L, "LKOH", new BigDecimal("6800.0000"));
        when(stockRepository.findAll()).thenReturn(List.of(lkoh));
        when(marketDataService.getCurrentPrice("LKOH")).thenReturn(new BigDecimal("7123.4500"));

        scheduler.refreshPrices();

        ArgumentCaptor<BigDecimal> priceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(stockRepository).updateCurrentPrice(eq(3L), priceCaptor.capture());
        assertThat(priceCaptor.getValue()).isEqualByComparingTo("7123.4500");
    }

    @DisplayName("refreshPrices: a failure for one ticker does not abort the rest")
    @Test
    void singleFailureDoesNotAbortRun() {
        Stock sber = stock(1L, "SBER", new BigDecimal("290.5000"));
        Stock lkoh = stock(3L, "LKOH", new BigDecimal("6800.0000"));
        Stock ydex = stock(4L, "YDEX", new BigDecimal("4100.0000"));
        when(stockRepository.findAll()).thenReturn(List.of(sber, lkoh, ydex));
        when(marketDataService.getCurrentPrice("SBER")).thenReturn(new BigDecimal("300.0000"));
        when(marketDataService.getCurrentPrice("LKOH"))
                .thenThrow(new MarketDataUnavailableException("MOEX ISS unreachable for ticker=LKOH"));
        when(marketDataService.getCurrentPrice("YDEX")).thenReturn(new BigDecimal("4200.0000"));

        scheduler.refreshPrices();

        // the two successful tickers were updated ...
        verify(stockRepository).updateCurrentPrice(eq(1L), eq(new BigDecimal("300.0000")));
        verify(stockRepository).updateCurrentPrice(eq(4L), eq(new BigDecimal("4200.0000")));
        // ... and the failing one was never written
        verify(stockRepository, never()).updateCurrentPrice(eq(3L), any(BigDecimal.class));
    }

    @DisplayName("refreshPrices: empty stock list — no calls, no errors")
    @Test
    void emptyStockListIsNoOp() {
        when(stockRepository.findAll()).thenReturn(List.of());

        scheduler.refreshPrices();

        verify(marketDataService, never()).getCurrentPrice(any());
        verify(stockRepository, never()).updateCurrentPrice(anyLong(), any(BigDecimal.class));
    }

    private Stock stock(long id, String ticker, BigDecimal price) {
        return Stock.builder()
                .id(id)
                .ticker(ticker)
                .companyName(ticker + " Inc.")
                .exchange("MOEX")
                .currentPrice(price)
                .build();
    }
}
