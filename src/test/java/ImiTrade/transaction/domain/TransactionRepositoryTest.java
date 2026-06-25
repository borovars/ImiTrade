package ImiTrade.transaction.domain;

import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer tests for {@link TransactionRepository}.
 *
 * <p>Runs against a real PostgreSQL container (via {@link ImiTrade.testsupport.PostgresTestBase})
 * because the {@code Transaction.type} column is a PostgreSQL {@code transaction_type}
 * enum, mapped with Hibernate's {@code @JdbcTypeCode(SqlTypes.NAMED_ENUM)}. H2 cannot
 * persist that mapping, so Flyway's real {@code V1..V3} migrations are applied to the
 * container (no {@code test} profile). Tickers here are deliberately unique
 * ({@code TXA}/{@code TXB}) to avoid colliding with the V2 seed.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest extends ImiTrade.testsupport.PostgresTestBase {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StockRepository stockRepository;

    private Long aliceId;
    private Long bobId;
    private Long stockAId;
    private Long stockBId;

    @BeforeEach
    void setUp() {
        User alice = userRepository.save(User.builder()
                .email("tx-alice@example.com").username("txalice")
                .passwordHash("$2a$10$hash").balance(new BigDecimal("500000.0000"))
                .createdAt(Instant.now()).build());
        User bob = userRepository.save(User.builder()
                .email("tx-bob@example.com").username("txbob")
                .passwordHash("$2a$10$hash").balance(new BigDecimal("500000.0000"))
                .createdAt(Instant.now()).build());
        aliceId = alice.getId();
        bobId = bob.getId();

        Stock stockA = stockRepository.save(Stock.builder()
                .ticker("TXA").companyName("Tx Alpha Inc.").exchange("NASDAQ")
                .currentPrice(new BigDecimal("100.0000")).build());
        Stock stockB = stockRepository.save(Stock.builder()
                .ticker("TXB").companyName("Tx Beta Inc.").exchange("NASDAQ")
                .currentPrice(new BigDecimal("200.0000")).build());
        stockAId = stockA.getId();
        stockBId = stockB.getId();

        Instant base = Instant.parse("2026-06-25T10:00:00Z");
        transactionRepository.saveAll(List.of(
                tx(aliceId, stockAId, TransactionType.BUY, 10, "100.0000", base.plusSeconds(60)),   // 10:01
                tx(aliceId, stockAId, TransactionType.SELL, 3, "110.0000", base.plusSeconds(120)),  // 10:02
                tx(aliceId, stockBId, TransactionType.BUY, 5, "200.0000", base.plusSeconds(180)),   // 10:03
                tx(bobId, stockAId, TransactionType.BUY, 7, "100.0000", base.plusSeconds(240)),     // 10:04
                tx(aliceId, stockBId, TransactionType.SELL, 2, "210.0000", base.plusSeconds(300))   // 10:05
        ));
    }

    @DisplayName("filter by userId returns only that user's transactions")
    @Test
    void filterByUserId() {
        Specification<Transaction> spec = TransactionSpecifications.hasUserId(aliceId);

        Page<Transaction> page = transactionRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(4);
        assertThat(page.getContent()).extracting(Transaction::getUserId)
                .containsOnly(aliceId);
    }

    @DisplayName("filter by userId isolates other users' transactions")
    @Test
    void filterByUserIdIsolatesByUser() {
        Page<Transaction> page = transactionRepository.findAll(
                TransactionSpecifications.hasUserId(bobId), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserId()).isEqualTo(bobId);
    }

    @DisplayName("filter by type (BUY) returns only BUY transactions of the user")
    @Test
    void filterByType() {
        Specification<Transaction> spec = Specification.allOf(
                TransactionSpecifications.hasUserId(aliceId),
                TransactionSpecifications.hasType(TransactionType.BUY));

        Page<Transaction> page = transactionRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Transaction::getType)
                .containsOnly(TransactionType.BUY);
    }

    @DisplayName("filter by stockId returns only transactions for that stock")
    @Test
    void filterByStockId() {
        Specification<Transaction> spec = Specification.allOf(
                TransactionSpecifications.hasUserId(aliceId),
                TransactionSpecifications.hasStockId(stockBId));

        Page<Transaction> page = transactionRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Transaction::getStockId)
                .containsOnly(stockBId);
    }

    @DisplayName("combining userId, type and stockId narrows to a single match")
    @Test
    void combineAllFilters() {
        Specification<Transaction> spec = Specification.allOf(
                TransactionSpecifications.hasUserId(aliceId),
                TransactionSpecifications.hasType(TransactionType.SELL),
                TransactionSpecifications.hasStockId(stockBId));

        Page<Transaction> page = transactionRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        Transaction tx = page.getContent().get(0);
        assertThat(tx.getType()).isEqualTo(TransactionType.SELL);
        assertThat(tx.getStockId()).isEqualTo(stockBId);
        assertThat(tx.getUserId()).isEqualTo(aliceId);
    }

    @DisplayName("pagination + createdAt DESC returns the expected slices and totals")
    @Test
    void paginationAndSorting() {
        Specification<Transaction> spec = TransactionSpecifications.hasUserId(aliceId);

        Page<Transaction> firstPage = transactionRepository.findAll(spec,
                PageRequest.of(0, 2, Sort.by("createdAt").descending()));
        Page<Transaction> secondPage = transactionRepository.findAll(spec,
                PageRequest.of(1, 2, Sort.by("createdAt").descending()));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        // newest first -> 10:05 (stockB SELL), then 10:03 (stockB BUY)
        assertThat(firstPage.getContent()).extracting(Transaction::getStockId)
                .containsExactly(stockBId, stockBId);

        assertThat(secondPage.getContent()).hasSize(2);
        // 10:02 (stockA SELL), then 10:01 (stockA BUY)
        assertThat(secondPage.getContent()).extracting(Transaction::getStockId)
                .containsExactly(stockAId, stockAId);
    }

    @DisplayName("no filters produce an empty page for a user with no history")
    @Test
    void emptyResult() {
        Long strangerId = aliceId + bobId + 1000L;
        Page<Transaction> page = transactionRepository.findAll(
                TransactionSpecifications.hasUserId(strangerId), PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    private Transaction tx(Long userId, Long stockId, TransactionType type,
                           int quantity, String price, Instant createdAt) {
        BigDecimal p = new BigDecimal(price);
        return Transaction.builder()
                .userId(userId)
                .stockId(stockId)
                .type(type)
                .quantity(quantity)
                .price(p)
                .totalAmount(p.multiply(BigDecimal.valueOf(quantity)))
                .createdAt(createdAt)
                .build();
    }
}
