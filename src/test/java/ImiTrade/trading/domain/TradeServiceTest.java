package ImiTrade.trading.domain;

import ImiTrade.common.exception.InsufficientBalanceException;
import ImiTrade.common.exception.InsufficientStockQuantityException;
import ImiTrade.common.exception.InvalidQuantityException;
import ImiTrade.common.exception.PortfolioPositionNotFoundException;
import ImiTrade.common.exception.StockNotFoundException;
import ImiTrade.portfolio.domain.PortfolioPosition;
import ImiTrade.portfolio.domain.PortfolioPositionRepository;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockService;
import ImiTrade.transaction.domain.Transaction;
import ImiTrade.transaction.domain.TransactionRepository;
import ImiTrade.transaction.domain.TransactionType;
import ImiTrade.trading.dto.BuyStockRequest;
import ImiTrade.trading.dto.SellStockRequest;
import ImiTrade.trading.dto.TradeResponse;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TradeService}. The collaborators (UserService,
 * StockService, repositories) are mocked; {@code transactionRepository.save} is
 * stubbed to assign an id so the returned {@link TradeResponse} is populated.
 */
@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long STOCK_ID = 1L;

    @Mock
    private UserService userService;
    @Mock
    private StockService stockService;
    @Mock
    private PortfolioPositionRepository portfolioPositionRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TradeService tradeService;

    @BeforeEach
    void setUp() {
        tradeService = new TradeService(userService, stockService, portfolioPositionRepository, transactionRepository);
    }

    // =========================================================================
    // BUY
    // =========================================================================

    @DisplayName("buy: success creates a BUY transaction, a new position and debits the balance")
    @Test
    void buySuccessNewPosition() {
        User user = sampleUser(new BigDecimal("500000.0000"));
        Stock stock = sampleStock(new BigDecimal("212.3500"));
        stubUserAndStock(user, stock);
        when(portfolioPositionRepository.findByUserIdAndStockId(USER_ID, STOCK_ID)).thenReturn(Optional.empty());
        stubTransactionSave(TransactionType.BUY);

        TradeResponse res = tradeService.buy(USER_ID, new BuyStockRequest(STOCK_ID, 10));

        assertThat(res.type()).isEqualTo("BUY");
        assertThat(res.stockTicker()).isEqualTo("AAPL");
        assertThat(res.quantity()).isEqualTo(10);
        assertThat(res.price()).isEqualByComparingTo("212.3500");
        assertThat(res.totalAmount()).isEqualByComparingTo("2123.5000");

        // balance debited
        assertThat(user.getBalance()).isEqualByComparingTo(new BigDecimal("500000.0000").subtract(new BigDecimal("2123.5000")));
        // a new position was saved
        verify(portfolioPositionRepository).save(any(PortfolioPosition.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @DisplayName("buy: existing position increases quantity and recalculates averagePrice")
    @Test
    void buyUpdatesAveragePrice() {
        User user = sampleUser(new BigDecimal("500000.0000"));
        Stock stock = sampleStock(new BigDecimal("200.0000"));
        stubUserAndStock(user, stock);

        // existing: 10 @ 100.0000; buy 10 @ 200.0000 -> avg = 150.0000, qty = 20
        PortfolioPosition existing = PortfolioPosition.builder()
                .id(7L).userId(USER_ID).stockId(STOCK_ID)
                .quantity(10).averagePrice(new BigDecimal("100.0000")).build();
        when(portfolioPositionRepository.findByUserIdAndStockId(USER_ID, STOCK_ID)).thenReturn(Optional.of(existing));
        stubTransactionSave(TransactionType.BUY);

        tradeService.buy(USER_ID, new BuyStockRequest(STOCK_ID, 10));

        assertThat(existing.getQuantity()).isEqualTo(20);
        assertThat(existing.getAveragePrice()).isEqualByComparingTo(new BigDecimal("150.0000"));
        verify(portfolioPositionRepository).save(existing);
    }

    @DisplayName("buy: insufficient balance throws and persists nothing")
    @Test
    void buyInsufficientBalance() {
        User user = sampleUser(new BigDecimal("100.0000"));
        Stock stock = sampleStock(new BigDecimal("212.3500"));
        stubUserAndStock(user, stock);

        assertThatThrownBy(() -> tradeService.buy(USER_ID, new BuyStockRequest(STOCK_ID, 10)))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(portfolioPositionRepository, never()).save(any(PortfolioPosition.class));
    }

    @DisplayName("buy: unknown stock propagates StockNotFoundException")
    @Test
    void buyUnknownStock() {
        User user = sampleUser(new BigDecimal("500000.0000"));
        when(userService.getById(USER_ID)).thenReturn(user);
        when(stockService.getStockById(STOCK_ID)).thenThrow(new StockNotFoundException(STOCK_ID));

        assertThatThrownBy(() -> tradeService.buy(USER_ID, new BuyStockRequest(STOCK_ID, 10)))
                .isInstanceOf(StockNotFoundException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @DisplayName("buy: quantity <= 0 throws InvalidQuantityException before any IO")
    @Test
    void buyInvalidQuantity() {
        assertThatThrownBy(() -> tradeService.buy(USER_ID, new BuyStockRequest(STOCK_ID, 0)))
                .isInstanceOf(InvalidQuantityException.class);

        verify(userService, never()).getById(any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // =========================================================================
    // SELL
    // =========================================================================

    @DisplayName("sell: success creates a SELL transaction, decreases quantity and credits the balance")
    @Test
    void sellSuccess() {
        User user = sampleUser(new BigDecimal("100000.0000"));
        Stock stock = sampleStock(new BigDecimal("212.3500"));
        stubUserAndStock(user, stock);

        PortfolioPosition position = PortfolioPosition.builder()
                .id(7L).userId(USER_ID).stockId(STOCK_ID)
                .quantity(10).averagePrice(new BigDecimal("100.0000")).build();
        when(portfolioPositionRepository.findByUserIdAndStockId(USER_ID, STOCK_ID)).thenReturn(Optional.of(position));
        stubTransactionSave(TransactionType.SELL);

        TradeResponse res = tradeService.sell(USER_ID, new SellStockRequest(STOCK_ID, 5));

        assertThat(res.type()).isEqualTo("SELL");
        assertThat(res.totalAmount()).isEqualByComparingTo("1061.7500");
        assertThat(position.getQuantity()).isEqualTo(5);
        // average price unchanged on sell
        assertThat(position.getAveragePrice()).isEqualByComparingTo(new BigDecimal("100.0000"));
        // balance credited
        assertThat(user.getBalance()).isEqualByComparingTo(new BigDecimal("100000.0000").add(new BigDecimal("1061.7500")));
        verify(portfolioPositionRepository).save(position);
    }

    @DisplayName("sell: remaining quantity 0 deletes the position")
    @Test
    void sellDeletesPositionWhenZero() {
        User user = sampleUser(new BigDecimal("100000.0000"));
        Stock stock = sampleStock(new BigDecimal("212.3500"));
        stubUserAndStock(user, stock);

        PortfolioPosition position = PortfolioPosition.builder()
                .id(7L).userId(USER_ID).stockId(STOCK_ID)
                .quantity(5).averagePrice(new BigDecimal("100.0000")).build();
        when(portfolioPositionRepository.findByUserIdAndStockId(USER_ID, STOCK_ID)).thenReturn(Optional.of(position));
        stubTransactionSave(TransactionType.SELL);

        tradeService.sell(USER_ID, new SellStockRequest(STOCK_ID, 5));

        verify(portfolioPositionRepository).delete(position);
        verify(portfolioPositionRepository, never()).save(any(PortfolioPosition.class));
    }

    @DisplayName("sell: no position throws PortfolioPositionNotFoundException")
    @Test
    void sellNoPosition() {
        User user = sampleUser(new BigDecimal("100000.0000"));
        when(userService.getById(USER_ID)).thenReturn(user);
        when(portfolioPositionRepository.findByUserIdAndStockId(USER_ID, STOCK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.sell(USER_ID, new SellStockRequest(STOCK_ID, 5)))
                .isInstanceOf(PortfolioPositionNotFoundException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @DisplayName("sell: insufficient shares throws InsufficientStockQuantityException")
    @Test
    void sellInsufficientQuantity() {
        User user = sampleUser(new BigDecimal("100000.0000"));
        when(userService.getById(USER_ID)).thenReturn(user);

        PortfolioPosition position = PortfolioPosition.builder()
                .id(7L).userId(USER_ID).stockId(STOCK_ID)
                .quantity(3).averagePrice(new BigDecimal("100.0000")).build();
        when(portfolioPositionRepository.findByUserIdAndStockId(USER_ID, STOCK_ID)).thenReturn(Optional.of(position));

        assertThatThrownBy(() -> tradeService.sell(USER_ID, new SellStockRequest(STOCK_ID, 5)))
                .isInstanceOf(InsufficientStockQuantityException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(portfolioPositionRepository, never()).save(any(PortfolioPosition.class));
    }

    @DisplayName("sell: quantity <= 0 throws InvalidQuantityException before any IO")
    @Test
    void sellInvalidQuantity() {
        assertThatThrownBy(() -> tradeService.sell(USER_ID, new SellStockRequest(STOCK_ID, 0)))
                .isInstanceOf(InvalidQuantityException.class);

        verify(userService, never()).getById(any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void stubUserAndStock(User user, Stock stock) {
        when(userService.getById(USER_ID)).thenReturn(user);
        when(stockService.getStockById(STOCK_ID)).thenReturn(stock);
    }

    private void stubTransactionSave(TransactionType type) {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            tx.setId(15L);
            return tx;
        });
    }

    private static User sampleUser(BigDecimal balance) {
        return User.builder()
                .id(USER_ID)
                .email("alice@example.com")
                .username("alice")
                .passwordHash("$2a$10$hash")
                .balance(balance)
                .createdAt(Instant.now())
                .build();
    }

    private static Stock sampleStock(BigDecimal currentPrice) {
        return Stock.builder()
                .id(STOCK_ID)
                .ticker("AAPL")
                .companyName("Apple Inc.")
                .exchange("NASDAQ")
                .currentPrice(currentPrice)
                .build();
    }
}
