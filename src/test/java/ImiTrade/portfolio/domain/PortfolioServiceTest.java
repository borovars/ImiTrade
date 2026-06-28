package ImiTrade.portfolio.domain;

import ImiTrade.portfolio.dto.PortfolioResponse;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PortfolioService}. Repository access is mocked, so these
 * tests focus on pnl computation, DTO mapping and the empty-portfolio path.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioPositionRepository portfolioPositionRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    @DisplayName("getPortfolio: enriches positions with the live stock and computes pnl")
    @Test
    void getPortfolioComputesPnlAndMapsDto() {
        // averagePrice = 210.50, currentPrice = 215.10, quantity = 10 -> pnl = 46.0000
        PortfolioPosition position = PortfolioPosition.builder()
                .id(1L).userId(7L).stockId(1L)
                .quantity(10)
                .averagePrice(new BigDecimal("210.5000"))
                .build();
        Stock stock = Stock.builder()
                .id(1L).ticker("SBER").companyName("Сбербанк").exchange("MOEX")
                .currentPrice(new BigDecimal("215.1000"))
                .build();
        when(portfolioPositionRepository.findByUserId(7L)).thenReturn(List.of(position));
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock));

        List<PortfolioResponse> result = portfolioService.getPortfolio(7L);

        assertThat(result).hasSize(1);
        PortfolioResponse line = result.get(0);
        assertThat(line.stockId()).isEqualTo(1L);
        assertThat(line.ticker()).isEqualTo("SBER");
        assertThat(line.companyName()).isEqualTo("Сбербанк");
        assertThat(line.quantity()).isEqualTo(10);
        assertThat(line.averagePrice()).isEqualByComparingTo("210.5000");
        assertThat(line.currentPrice()).isEqualByComparingTo("215.1000");
        assertThat(line.pnl()).isEqualByComparingTo("46.0000");

        verify(portfolioPositionRepository).findByUserId(7L);
        verify(stockRepository).findAllById(any());
    }

    @DisplayName("getPortfolio: pnl is negative when the price has dropped")
    @Test
    void getPortfolioNegativePnl() {
        // averagePrice = 215.10, currentPrice = 210.50, quantity = 10 -> pnl = -46.0000
        PortfolioPosition position = PortfolioPosition.builder()
                .id(1L).userId(7L).stockId(1L)
                .quantity(10)
                .averagePrice(new BigDecimal("215.1000"))
                .build();
        Stock stock = Stock.builder()
                .id(1L).ticker("SBER").companyName("Сбербанк").exchange("MOEX")
                .currentPrice(new BigDecimal("210.5000"))
                .build();
        when(portfolioPositionRepository.findByUserId(7L)).thenReturn(List.of(position));
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock));

        List<PortfolioResponse> result = portfolioService.getPortfolio(7L);

        assertThat(result.get(0).pnl()).isEqualByComparingTo("-46.0000");
    }

    @DisplayName("getPortfolio: several positions are each enriched independently")
    @Test
    void getPortfolioMultiplePositions() {
        PortfolioPosition p1 = PortfolioPosition.builder()
                .id(1L).userId(7L).stockId(1L)
                .quantity(10).averagePrice(new BigDecimal("210.5000")).build();
        PortfolioPosition p2 = PortfolioPosition.builder()
                .id(2L).userId(7L).stockId(2L)
                .quantity(5).averagePrice(new BigDecimal("400.0000")).build();
        Stock s1 = Stock.builder().id(1L).ticker("SBER").companyName("Сбербанк")
                .exchange("MOEX").currentPrice(new BigDecimal("215.1000")).build();
        Stock s2 = Stock.builder().id(2L).ticker("GAZP").companyName("Газпром")
                .exchange("MOEX").currentPrice(new BigDecimal("420.0000")).build();
        when(portfolioPositionRepository.findByUserId(7L)).thenReturn(List.of(p1, p2));
        when(stockRepository.findAllById(any())).thenReturn(List.of(s1, s2));

        List<PortfolioResponse> result = portfolioService.getPortfolio(7L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PortfolioResponse::ticker).containsExactlyInAnyOrder("SBER", "GAZP");
        assertThat(result).extracting(PortfolioResponse::pnl)
                .containsExactlyInAnyOrder(
                        new BigDecimal("46.0000"),   // (215.1 - 210.5) * 10
                        new BigDecimal("100.0000")); // (420.0 - 400.0) * 5
    }

    @DisplayName("getPortfolio: empty portfolio returns an empty list and skips stock lookup")
    @Test
    void getPortfolioEmpty() {
        when(portfolioPositionRepository.findByUserId(7L)).thenReturn(List.of());

        List<PortfolioResponse> result = portfolioService.getPortfolio(7L);

        assertThat(result).isEmpty();
        verify(stockRepository, never()).findAllById(any());
    }

    @DisplayName("getPortfolio: never returns null — always a list instance")
    @Test
    void getPortfolioNeverReturnsNull() {
        when(portfolioPositionRepository.findByUserId(7L)).thenReturn(List.of());

        assertThat(portfolioService.getPortfolio(7L)).isNotNull();
    }

    /** Regression guard: the service must rely solely on the persisted average price. */
    @Test
    @DisplayName("getPortfolio: average price comes straight from the position, not recomputed")
    void getPortfolioUsesPersistedAveragePrice() {
        PortfolioPosition position = PortfolioPosition.builder()
                .id(1L).userId(7L).stockId(1L)
                .quantity(2).averagePrice(new BigDecimal("100.0000")).build();
        Stock stock = Stock.builder().id(1L).ticker("SBER").companyName("Сбербанк")
                .exchange("MOEX").currentPrice(new BigDecimal("150.0000")).build();
        when(portfolioPositionRepository.findByUserId(7L)).thenReturn(List.of(position));
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock));

        PortfolioResponse line = portfolioService.getPortfolio(7L).get(0);

        assertThat(line.averagePrice()).isEqualByComparingTo("100.0000");
        assertThat(line.pnl()).isEqualByComparingTo("100.0000"); // (150 - 100) * 2
    }

    @Test
    @DisplayName("getPortfolio: pnl zeroed when current price equals the average price")
    void getPortfolioZeroPnl() {
        PortfolioPosition position = PortfolioPosition.builder()
                .id(1L).userId(7L).stockId(1L)
                .quantity(7).averagePrice(new BigDecimal("200.0000")).build();
        Stock stock = Stock.builder().id(1L).ticker("SBER").companyName("Сбербанк")
                .exchange("MOEX").currentPrice(new BigDecimal("200.0000")).build();
        when(portfolioPositionRepository.findByUserId(7L)).thenReturn(List.of(position));
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock));

        Optional<BigDecimal> pnl = Optional.of(portfolioService.getPortfolio(7L).get(0).pnl());

        assertThat(pnl).hasValue(new BigDecimal("0.0000"));
    }
}
