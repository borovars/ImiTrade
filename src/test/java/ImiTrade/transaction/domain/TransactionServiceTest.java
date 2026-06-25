package ImiTrade.transaction.domain;

import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import ImiTrade.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private TransactionService transactionService;

    @DisplayName("getTransactions: returns the user's history as DTOs with resolved tickers")
    @Test
    void getTransactionsReturnsHistory() {
        Pageable pageable = PageRequest.of(0, 20);
        Transaction buy = transaction(10L, 1L, TransactionType.BUY, 10, "210.5000");
        Transaction sell = transaction(11L, 1L, TransactionType.SELL, 3, "215.0000");
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(buy, sell), pageable, 2));
        when(stockRepository.findAllById(any()))
                .thenReturn(List.of(stock(1L, "AAPL")));

        Page<TransactionResponse> result = transactionService.getTransactions(USER_ID, null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        TransactionResponse first = result.getContent().get(0);
        assertThat(first.id()).isEqualTo(10L);
        assertThat(first.stockId()).isEqualTo(1L);
        assertThat(first.ticker()).isEqualTo("AAPL");
        assertThat(first.type()).isEqualTo("BUY");
        assertThat(first.quantity()).isEqualTo(10);
        assertThat(first.totalAmount()).isEqualByComparingTo("2105.0000");
        // never exposes the entity directly
        assertThat(result.getContent()).allSatisfy(r -> assertThat(r).isInstanceOf(TransactionResponse.class));
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @DisplayName("getTransactions: type filter is forwarded to the repository")
    @Test
    void getTransactionsWithTypeFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Transaction buy = transaction(10L, 1L, TransactionType.BUY, 10, "210.5000");
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(buy), pageable, 1));
        when(stockRepository.findAllById(any()))
                .thenReturn(List.of(stock(1L, "AAPL")));

        Page<TransactionResponse> result =
                transactionService.getTransactions(USER_ID, TransactionType.BUY, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).type()).isEqualTo("BUY");
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @DisplayName("getTransactions: stockId filter is forwarded to the repository")
    @Test
    void getTransactionsWithStockIdFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Transaction buy = transaction(10L, 1L, TransactionType.BUY, 10, "210.5000");
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(buy), pageable, 1));
        when(stockRepository.findAllById(any()))
                .thenReturn(List.of(stock(1L, "AAPL")));

        Page<TransactionResponse> result =
                transactionService.getTransactions(USER_ID, null, 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).stockId()).isEqualTo(1L);
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @DisplayName("getTransactions: empty history returns an empty page without resolving tickers")
    @Test
    void getTransactionsEmpty() {
        Pageable pageable = PageRequest.of(0, 20);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<TransactionResponse> result =
                transactionService.getTransactions(USER_ID, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ---- helpers ----

    private Transaction transaction(Long id, Long stockId, TransactionType type, int qty, String price) {
        BigDecimal p = new BigDecimal(price);
        return Transaction.builder()
                .id(id)
                .userId(USER_ID)
                .stockId(stockId)
                .type(type)
                .quantity(qty)
                .price(p)
                .totalAmount(p.multiply(BigDecimal.valueOf(qty)))
                .createdAt(Instant.parse("2026-06-25T10:15:00Z"))
                .build();
    }

    private Stock stock(Long id, String ticker) {
        return Stock.builder()
                .id(id)
                .ticker(ticker)
                .companyName("C")
                .exchange("NASDAQ")
                .currentPrice(new BigDecimal("1.0000"))
                .build();
    }
}
