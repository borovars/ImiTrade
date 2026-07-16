package ImiTrade.portfolio.domain;

import ImiTrade.portfolio.dto.PortfolioHistoryResponse;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import ImiTrade.stocks.dto.CandleResponse;
import ImiTrade.stocks.service.HistoryPeriod;
import ImiTrade.stocks.service.StockHistoryService;
import ImiTrade.transaction.domain.Transaction;
import ImiTrade.transaction.domain.TransactionRepository;
import ImiTrade.transaction.domain.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the portfolio-value history reconstruction in
 * {@link PortfolioService#getHistory}.
 *
 * <p>Uses the explicit-constructor + fixed-clock pattern (mirroring
 * {@code StockHistoryServiceTest}) rather than {@code @InjectMocks}, so the
 * {@link Clock} dependency is wired deterministically. {@link StockHistoryService}
 * is mocked: each test stubs it to return a fixed candle series per ticker, and we
 * assert that {@code getHistory} replays the user's transactions against those
 * closes to produce {@code value = Σ quantity_held × close} at every timestamp.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioHistoryServiceTest {

    private static final long USER_ID = 7L;
    private static final long SBER_ID = 1L;
    private static final long GAZP_ID = 2L;

    /** Fixed clock at 2026-07-07T12:00:00Z (unused by getHistory except for injection parity). */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-07T12:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private PortfolioPositionRepository portfolioPositionRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private StockHistoryService stockHistoryService;

    private PortfolioService service() {
        return new PortfolioService(portfolioPositionRepository, stockRepository,
                transactionRepository, stockHistoryService, FIXED_CLOCK);
    }

    private static Stock stock(long id, String ticker) {
        return Stock.builder().id(id).ticker(ticker).companyName(ticker).exchange("MOEX")
                .currentPrice(new BigDecimal("100.0000")).lotSize(1).build();
    }

    private static CandleResponse candle(String time, String close) {
        return new CandleResponse(Instant.parse(time), null, null, null,
                new BigDecimal(close), null);
    }

    private static Transaction buy(long stockId, int qty, String time) {
        return Transaction.builder()
                .id(null).userId(USER_ID).stockId(stockId).type(TransactionType.BUY)
                .quantity(qty).price(new BigDecimal("100.0000"))
                .totalAmount(new BigDecimal("100.0000"))
                .createdAt(Instant.parse(time)).build();
    }

    private static Transaction sell(long stockId, int qty, String time) {
        return Transaction.builder()
                .id(null).userId(USER_ID).stockId(stockId).type(TransactionType.SELL)
                .quantity(qty).price(new BigDecimal("100.0000"))
                .totalAmount(new BigDecimal("100.0000"))
                .createdAt(Instant.parse(time)).build();
    }

    @Test
    @DisplayName("getHistory: empty when the user has never traded")
    void emptyWhenNoTransactions() {
        when(transactionRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of());

        List<PortfolioHistoryResponse> result = service().getHistory(USER_ID, HistoryPeriod.D1, null);

        assertThat(result).isEmpty();
        verify(stockHistoryService, never()).getHistory(any(), any(), any());
    }

    @Test
    @DisplayName("getHistory: single stock, single buy — value tracks the close price × quantity")
    void singleStockSingleBuy() {
        when(transactionRepository.findByUserIdOrderByCreatedAtAsc(USER_ID))
                .thenReturn(List.of(buy(SBER_ID, 10, "2026-07-01T10:00:00Z")));
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock(SBER_ID, "SBER")));
        when(stockHistoryService.getHistory(eq("SBER"), eq(HistoryPeriod.D1), any()))
                .thenReturn(List.of(
                        candle("2026-07-01T07:00:00Z", "100.0000"),
                        candle("2026-07-02T07:00:00Z", "102.0000"),
                        candle("2026-07-03T07:00:00Z", "98.0000")));

        List<PortfolioHistoryResponse> result = service().getHistory(USER_ID, HistoryPeriod.D1, null);

        // quantity 10 held on all three days (bought at 2026-07-01T10:00, all candles are at 07:00
        // the same day onward; the 07-01 candle is at 07:00 which is BEFORE the 10:00 buy — but
        // the service skips timestamps where qty<=0, so only 07-02 and 07-03 are emitted).
        assertThat(result).extracting(PortfolioHistoryResponse::time).containsExactly(
                Instant.parse("2026-07-02T07:00:00Z"),
                Instant.parse("2026-07-03T07:00:00Z"));
        assertThat(result).extracting(PortfolioHistoryResponse::value)
                .containsExactly(
                        new BigDecimal("1020.0000"), // 10 × 102
                        new BigDecimal("980.0000")); // 10 × 98
    }

    @Test
    @DisplayName("getHistory: several stocks — sums each held position's market value per timestamp")
    void severalStocks() {
        when(transactionRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(
                buy(SBER_ID, 10, "2026-06-29T10:00:00Z"),
                buy(GAZP_ID, 5, "2026-06-29T10:00:00Z")));
        when(stockRepository.findAllById(any())).thenReturn(
                List.of(stock(SBER_ID, "SBER"), stock(GAZP_ID, "GAZP")));
        when(stockHistoryService.getHistory(eq("SBER"), eq(HistoryPeriod.D1), any()))
                .thenReturn(List.of(
                        candle("2026-07-01T07:00:00Z", "100.0000"),
                        candle("2026-07-02T07:00:00Z", "110.0000")));
        when(stockHistoryService.getHistory(eq("GAZP"), eq(HistoryPeriod.D1), any()))
                .thenReturn(List.of(
                        candle("2026-07-01T07:00:00Z", "200.0000"),
                        candle("2026-07-02T07:00:00Z", "220.0000")));

        List<PortfolioHistoryResponse> result = service().getHistory(USER_ID, HistoryPeriod.D1, null);

        assertThat(result).extracting(PortfolioHistoryResponse::value)
                .containsExactly(
                        new BigDecimal("2000.0000"), // 10×100 + 5×200
                        new BigDecimal("2200.0000")); // 10×110 + 5×220
    }

    @Test
    @DisplayName("getHistory: multiple buys of the same stock accumulate the held quantity")
    void multipleBuysAccumulate() {
        when(transactionRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(
                buy(SBER_ID, 10, "2026-06-29T10:00:00Z"),
                buy(SBER_ID, 5, "2026-07-01T12:00:00Z")));
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock(SBER_ID, "SBER")));
        when(stockHistoryService.getHistory(eq("SBER"), eq(HistoryPeriod.D1), any()))
                .thenReturn(List.of(
                        candle("2026-07-01T07:00:00Z", "100.0000"), // before 2nd buy: qty=10
                        candle("2026-07-02T07:00:00Z", "100.0000"), // after 2nd buy:  qty=15
                        candle("2026-07-03T07:00:00Z", "100.0000")));

        List<PortfolioHistoryResponse> result = service().getHistory(USER_ID, HistoryPeriod.D1, null);

        assertThat(result).extracting(PortfolioHistoryResponse::value)
                .containsExactly(
                        new BigDecimal("1000.0000"), // 10 × 100
                        new BigDecimal("1500.0000"), // 15 × 100
                        new BigDecimal("1500.0000"));
    }

    @Test
    @DisplayName("getHistory: partial sell decrements the held quantity (line continues)")
    void partialSell() {
        when(transactionRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(
                buy(SBER_ID, 10, "2026-06-29T10:00:00Z"),
                sell(SBER_ID, 4, "2026-07-01T12:00:00Z")));
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock(SBER_ID, "SBER")));
        when(stockHistoryService.getHistory(eq("SBER"), eq(HistoryPeriod.D1), any()))
                .thenReturn(List.of(
                        candle("2026-07-01T07:00:00Z", "100.0000"), // before sell: qty=10
                        candle("2026-07-02T07:00:00Z", "100.0000"), // after sell:  qty=6
                        candle("2026-07-03T07:00:00Z", "100.0000")));

        List<PortfolioHistoryResponse> result = service().getHistory(USER_ID, HistoryPeriod.D1, null);

        assertThat(result).extracting(PortfolioHistoryResponse::value)
                .containsExactly(
                        new BigDecimal("1000.0000"), // 10 × 100
                        new BigDecimal("600.0000"),  // 6 × 100
                        new BigDecimal("600.0000"));
    }

    @Test
    @DisplayName("getHistory: full sell — points after the sell-out are skipped (no flat zero line)")
    void fullSellSkipsZeroHoldings() {
        when(transactionRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(
                buy(SBER_ID, 10, "2026-06-29T10:00:00Z"),
                sell(SBER_ID, 10, "2026-07-01T12:00:00Z")));
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock(SBER_ID, "SBER")));
        when(stockHistoryService.getHistory(eq("SBER"), eq(HistoryPeriod.D1), any()))
                .thenReturn(List.of(
                        candle("2026-07-01T07:00:00Z", "100.0000"), // before sell: qty=10
                        candle("2026-07-02T07:00:00Z", "100.0000"), // after full sell: qty=0
                        candle("2026-07-03T07:00:00Z", "100.0000")));

        List<PortfolioHistoryResponse> result = service().getHistory(USER_ID, HistoryPeriod.D1, null);

        // Only the 07-01 point (still holding) is emitted; 07-02 and 07-03 (qty=0) are skipped.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).time()).isEqualTo(Instant.parse("2026-07-01T07:00:00Z"));
        assertThat(result.get(0).value()).isEqualByComparingTo("1000.0000");
    }

    @Test
    @DisplayName("getHistory: scales values to money scale 4 with HALF_UP rounding")
    void roundsToScale4() {
        when(transactionRepository.findByUserIdOrderByCreatedAtAsc(USER_ID))
                .thenReturn(List.of(buy(SBER_ID, 3, "2026-06-29T10:00:00Z")));
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock(SBER_ID, "SBER")));
        when(stockHistoryService.getHistory(eq("SBER"), eq(HistoryPeriod.D1), any()))
                .thenReturn(List.of(candle("2026-07-01T07:00:00Z", "99.9999")));

        List<PortfolioHistoryResponse> result = service().getHistory(USER_ID, HistoryPeriod.D1, null);

        // 3 × 99.9999 = 299.9997 → scale 4 HALF_UP = 299.9997
        assertThat(result.get(0).value()).isEqualByComparingTo("299.9997");
        assertThat(result.get(0).value().scale()).isEqualTo(4);
    }

    @Test
    @DisplayName("getHistory: caches each ticker's history — calls MOEX once per distinct ticker")
    void cachesHistoryPerTicker() {
        // Two BUYs of the same stockId must produce a SINGLE MOEX history call.
        when(transactionRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(
                buy(SBER_ID, 10, "2026-06-29T10:00:00Z"),
                buy(SBER_ID, 5, "2026-06-30T10:00:00Z")));
        when(stockRepository.findAllById(any())).thenReturn(List.of(stock(SBER_ID, "SBER")));
        when(stockHistoryService.getHistory(eq("SBER"), eq(HistoryPeriod.D1), any()))
                .thenReturn(List.of(candle("2026-07-01T07:00:00Z", "100.0000")));

        service().getHistory(USER_ID, HistoryPeriod.D1, null);

        verify(stockHistoryService).getHistory("SBER", HistoryPeriod.D1, null);
    }
}
