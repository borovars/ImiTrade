package ImiTrade.stocks.domain;

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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer tests for {@link StockRepository} on in-memory H2
 * (PostgreSQL-compatibility mode). Verifies the {@link StockSpecifications}
 * predicates and pagination, which back the public listing endpoint.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository.saveAll(List.of(
                Stock.builder().ticker("AAPL").companyName("Apple Inc.").exchange("NASDAQ").currentPrice(new BigDecimal("212.3500")).build(),
                Stock.builder().ticker("MSFT").companyName("Microsoft Corporation").exchange("NASDAQ").currentPrice(new BigDecimal("415.2000")).build(),
                Stock.builder().ticker("NVDA").companyName("NVIDIA Corporation").exchange("NASDAQ").currentPrice(new BigDecimal("120.4500")).build(),
                Stock.builder().ticker("AMZN").companyName("Amazon.com Inc.").exchange("NASDAQ").currentPrice(new BigDecimal("185.7000")).build()
        ));
    }

    @DisplayName("filter by ticker (case-insensitive exact match) returns the single match")
    @Test
    void filterByTicker() {
        Specification<Stock> spec = StockSpecifications.hasTickerIgnoreCase("aapl");

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTicker()).isEqualTo("AAPL");
    }

    @DisplayName("filter by ticker with no match returns an empty page")
    @Test
    void filterByTickerNoMatch() {
        Specification<Stock> spec = StockSpecifications.hasTickerIgnoreCase("NOPE");

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
    }

    @DisplayName("filter by companyName (case-insensitive partial match) returns all matches")
    @Test
    void filterByCompanyName() {
        Specification<Stock> spec = StockSpecifications.companyNameContainsIgnoreCase("inc");

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent())
                .extracting(Stock::getCompanyName)
                .containsExactlyInAnyOrder("Apple Inc.", "Amazon.com Inc.");
    }

    @DisplayName("combining ticker and companyName predicates narrows the result")
    @Test
    void combineTickerAndCompanyName() {
        Specification<Stock> spec = Specification.allOf(
                StockSpecifications.hasTickerIgnoreCase("AMZN"),
                StockSpecifications.companyNameContainsIgnoreCase("amazon"));

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTicker()).isEqualTo("AMZN");
    }

    @DisplayName("pagination returns the requested page slice and total count")
    @Test
    void pagination() {
        Page<Stock> firstPage = stockRepository.findAll(PageRequest.of(0, 2, Sort.by("ticker").ascending()));
        Page<Stock> secondPage = stockRepository.findAll(PageRequest.of(1, 2, Sort.by("ticker").ascending()));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(Stock::getTicker)
                .containsExactly("AAPL", "AMZN");

        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).extracting(Stock::getTicker)
                .containsExactly("MSFT", "NVDA");
    }
}
