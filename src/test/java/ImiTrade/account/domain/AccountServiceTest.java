package ImiTrade.account.domain;

import ImiTrade.account.dto.AccountResponse;
import ImiTrade.portfolio.domain.PortfolioPosition;
import ImiTrade.portfolio.domain.PortfolioPositionRepository;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AccountService}. Repository/service access is mocked, so these
 * tests focus on the in-memory computation of {@code portfolioValue}, {@code totalAssets},
 * {@code profitLoss} and {@code positionsCount}, plus the empty-portfolio path.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private UserService userService;

    @Mock
    private PortfolioPositionRepository portfolioPositionRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private AccountService accountService;

    @DisplayName("getCurrentAccount: aggregates portfolioValue, totalAssets, profitLoss and positionsCount across positions")
    @Test
    void getCurrentAccountComputesAggregates() {
        // balance = 12500.5000
        // p1: qty=10, avg=210.5000, current=215.1000 -> value=2151.0000, pnl=46.0000
        // p2: qty=5,  avg=400.0000, current=420.0000 -> value=2100.0000, pnl=100.0000
        when(userService.getById(USER_ID)).thenReturn(user(new BigDecimal("12500.5000")));

        PortfolioPosition p1 = PortfolioPosition.builder()
                .id(1L).userId(USER_ID).stockId(1L)
                .quantity(10).averagePrice(new BigDecimal("210.5000")).build();
        PortfolioPosition p2 = PortfolioPosition.builder()
                .id(2L).userId(USER_ID).stockId(2L)
                .quantity(5).averagePrice(new BigDecimal("400.0000")).build();
        when(portfolioPositionRepository.findByUserId(USER_ID)).thenReturn(List.of(p1, p2));

        Stock s1 = Stock.builder().id(1L).ticker("AAPL").companyName("Apple Inc.")
                .exchange("NASDAQ").currentPrice(new BigDecimal("215.1000")).build();
        Stock s2 = Stock.builder().id(2L).ticker("MSFT").companyName("Microsoft Corporation")
                .exchange("NASDAQ").currentPrice(new BigDecimal("420.0000")).build();
        when(stockRepository.findAllById(any())).thenReturn(List.of(s1, s2));

        AccountResponse result = accountService.getCurrentAccount(USER_ID);

        assertThat(result.username()).isEqualTo("arseny");
        assertThat(result.email()).isEqualTo("arseny@example.com");
        assertThat(result.balance()).isEqualByComparingTo("12500.5000");
        // 215.1000*10 + 420.0000*5 = 2151.0000 + 2100.0000
        assertThat(result.portfolioValue()).isEqualByComparingTo("4251.0000");
        // (215.1000-210.5000)*10 + (420.0000-400.0000)*5 = 46.0000 + 100.0000
        assertThat(result.profitLoss()).isEqualByComparingTo("146.0000");
        // 12500.5000 + 4251.0000
        assertThat(result.totalAssets()).isEqualByComparingTo("16751.5000");
        assertThat(result.positionsCount()).isEqualTo(2);

        verify(userService).getById(USER_ID);
        verify(portfolioPositionRepository).findByUserId(USER_ID);
        verify(stockRepository).findAllById(any());
    }

    @DisplayName("getCurrentAccount: empty portfolio zeroes portfolioValue/profitLoss, totalAssets equals balance")
    @Test
    void getCurrentAccountEmptyPortfolio() {
        when(userService.getById(USER_ID)).thenReturn(user(new BigDecimal("12500.5000")));
        when(portfolioPositionRepository.findByUserId(USER_ID)).thenReturn(List.of());

        AccountResponse result = accountService.getCurrentAccount(USER_ID);

        assertThat(result.username()).isEqualTo("arseny");
        assertThat(result.email()).isEqualTo("arseny@example.com");
        assertThat(result.balance()).isEqualByComparingTo("12500.5000");
        assertThat(result.portfolioValue()).isEqualByComparingTo("0.0000");
        assertThat(result.profitLoss()).isEqualByComparingTo("0.0000");
        assertThat(result.totalAssets()).isEqualByComparingTo("12500.5000");
        assertThat(result.positionsCount()).isZero();

        // the stock lookup must be skipped when there are no positions
        verify(stockRepository, never()).findAllById(any());
    }

    @DisplayName("getCurrentAccount: never returns null")
    @Test
    void getCurrentAccountNeverReturnsNull() {
        when(userService.getById(USER_ID)).thenReturn(user(new BigDecimal("12500.5000")));
        when(portfolioPositionRepository.findByUserId(USER_ID)).thenReturn(List.of());

        assertThat(accountService.getCurrentAccount(USER_ID)).isNotNull();
    }

    private static User user(BigDecimal balance) {
        return User.builder()
                .id(USER_ID)
                .email("arseny@example.com")
                .username("arseny")
                .passwordHash("hash")
                .balance(balance)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
