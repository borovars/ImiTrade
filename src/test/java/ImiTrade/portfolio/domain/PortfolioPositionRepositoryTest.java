package ImiTrade.portfolio.domain;

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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer tests for {@link PortfolioPositionRepository} on in-memory H2
 * (PostgreSQL-compatibility mode). Verifies the per-user lookup that backs the
 * {@code GET /api/v1/portfolio} endpoint.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PortfolioPositionRepositoryTest {

    @Autowired
    private PortfolioPositionRepository portfolioPositionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StockRepository stockRepository;

    private Long aliceId;
    private Long bobId;
    private Long aaplId;

    @BeforeEach
    void setUp() {
        User alice = userRepository.save(User.builder()
                .email("alice@example.com").username("alice")
                .passwordHash("$2a$10$hash").balance(new BigDecimal("500000.0000"))
                .createdAt(Instant.now()).build());
        User bob = userRepository.save(User.builder()
                .email("bob@example.com").username("bob")
                .passwordHash("$2a$10$hash").balance(new BigDecimal("500000.0000"))
                .createdAt(Instant.now()).build());
        aliceId = alice.getId();
        bobId = bob.getId();

        Stock sber = stockRepository.save(Stock.builder()
                .ticker("SBER").companyName("Сбербанк").exchange("MOEX")
                .currentPrice(new BigDecimal("215.1000")).lotSize(1).build());
        aaplId = sber.getId();
        Stock gazp = stockRepository.save(Stock.builder()
                .ticker("GAZP").companyName("Газпром").exchange("MOEX")
                .currentPrice(new BigDecimal("420.0000")).lotSize(10).build());

        portfolioPositionRepository.saveAll(List.of(
                PortfolioPosition.builder()
                        .userId(aliceId).stockId(sber.getId())
                        .quantity(10).averagePrice(new BigDecimal("210.5000")).build(),
                PortfolioPosition.builder()
                        .userId(aliceId).stockId(gazp.getId())
                        .quantity(5).averagePrice(new BigDecimal("400.0000")).build(),
                PortfolioPosition.builder()
                        .userId(bobId).stockId(sber.getId())
                        .quantity(3).averagePrice(new BigDecimal("200.0000")).build()));
    }

    @DisplayName("findByUserId returns every position of the user")
    @Test
    void findByUserIdReturnsAllUserPositions() {
        List<PortfolioPosition> positions = portfolioPositionRepository.findByUserId(aliceId);

        assertThat(positions).hasSize(2);
        assertThat(positions).extracting(PortfolioPosition::getUserId)
                .containsOnly(aliceId);
        assertThat(positions).extracting(PortfolioPosition::getAveragePrice)
                .containsExactlyInAnyOrder(new BigDecimal("210.5000"), new BigDecimal("400.0000"));
    }

    @DisplayName("findByUserId does not leak other users' positions")
    @Test
    void findByUserIdIsolatesByUser() {
        List<PortfolioPosition> positions = portfolioPositionRepository.findByUserId(bobId);

        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).getUserId()).isEqualTo(bobId);
        assertThat(positions.get(0).getQuantity()).isEqualTo(3);
    }

    @DisplayName("findByUserId returns an empty list for a user with no holdings")
    @Test
    void findByUserIdEmpty() {
        Long strangerId = aliceId + bobId + 1000L;

        assertThat(portfolioPositionRepository.findByUserId(strangerId)).isEmpty();
    }

    @DisplayName("findByUserIdAndStockId still returns the single matching pair")
    @Test
    void findByUserIdAndStockIdContractIsPreserved() {
        // The existing finder must keep its contract after adding findByUserId.
        var pair = portfolioPositionRepository.findByUserIdAndStockId(aliceId, aaplId);

        assertThat(pair).isPresent();
        assertThat(pair.get().getQuantity()).isEqualTo(10);
        assertThat(pair.get().getAveragePrice()).isEqualByComparingTo("210.5000");
    }
}
