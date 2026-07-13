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
                Stock.builder().ticker("SBER").companyName("Сбербанк").exchange("MOEX").currentPrice(new BigDecimal("310.5000")).lotSize(1).build(),
                Stock.builder().ticker("GAZP").companyName("Газпром").exchange("MOEX").currentPrice(new BigDecimal("170.2000")).lotSize(10).build(),
                Stock.builder().ticker("LKOH").companyName("ЛУКОЙЛ").exchange("MOEX").currentPrice(new BigDecimal("6800.0000")).lotSize(1).build(),
                Stock.builder().ticker("ROSN").companyName("Роснефть").exchange("MOEX").currentPrice(new BigDecimal("620.0000")).lotSize(1).build()
        ));
    }

    @DisplayName("filter by ticker (case-insensitive exact match) returns the single match")
    @Test
    void filterByTicker() {
        Specification<Stock> spec = StockSpecifications.hasTickerIgnoreCase("sber");

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTicker()).isEqualTo("SBER");
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
        Specification<Stock> spec = StockSpecifications.companyNameContainsIgnoreCase("о");

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent())
                .extracting(Stock::getCompanyName)
                .containsExactlyInAnyOrder("Газпром", "ЛУКОЙЛ", "Роснефть");
    }

    @DisplayName("combining ticker and companyName predicates narrows the result")
    @Test
    void combineTickerAndCompanyName() {
        Specification<Stock> spec = Specification.allOf(
                StockSpecifications.hasTickerIgnoreCase("ROSN"),
                StockSpecifications.companyNameContainsIgnoreCase("роснефть"));

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTicker()).isEqualTo("ROSN");
    }

    @DisplayName("search by partial ticker (case-insensitive) returns matching stocks")
    @Test
    void searchByPartialTicker() {
        Specification<Stock> spec = StockSpecifications.tickerOrCompanyNameContainsIgnoreCase("ga");

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTicker()).isEqualTo("GAZP");
    }

    @DisplayName("search by partial company name (case-insensitive) returns matching stocks")
    @Test
    void searchByPartialCompanyName() {
        Specification<Stock> spec = StockSpecifications.tickerOrCompanyNameContainsIgnoreCase("нефть");

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCompanyName()).isEqualTo("Роснефть");
    }

    @DisplayName("search matches ticker OR company name across multiple stocks")
    @Test
    void searchMatchesTickerOrCompanyName() {
        // «о» встречается в company names Газпром/ЛУКОЙЛ/Роснефть; «sber» — тикер SBER.
        Specification<Stock> spec = StockSpecifications.tickerOrCompanyNameContainsIgnoreCase("sber");

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTicker()).isEqualTo("SBER");
    }

    @DisplayName("search with no match returns an empty page")
    @Test
    void searchNoMatch() {
        Specification<Stock> spec = StockSpecifications.tickerOrCompanyNameContainsIgnoreCase("zzz");

        Page<Stock> page = stockRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
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
                .containsExactly("GAZP", "LKOH");

        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).extracting(Stock::getTicker)
                .containsExactly("ROSN", "SBER");
    }

    @DisplayName("findAll returns every seeded stock (used by the price-refresh scheduler)")
    @Test
    void findAllReturnsAll() {
        List<Stock> all = stockRepository.findAll();

        assertThat(all).hasSize(4);
        assertThat(all).extracting(Stock::getTicker)
                .containsExactlyInAnyOrder("SBER", "GAZP", "LKOH", "ROSN");
    }

    @DisplayName("updateCurrentPrice writes the new price and returns 1 for an existing stock")
    @Test
    void updateCurrentPriceExisting() {
        Stock aapl = stockRepository.findAll(PageRequest.of(0, 20)).getContent().stream()
                .filter(s -> s.getTicker().equals("SBER"))
                .findFirst().orElseThrow();
        BigDecimal newPrice = new BigDecimal("999.0000");

        int affected = stockRepository.updateCurrentPrice(aapl.getId(), newPrice);

        assertThat(affected).isEqualTo(1);
        // flush so the bulk UPDATE is applied before the verification SELECT
        stockRepository.flush();
        BigDecimal persisted = stockRepository.findById(aapl.getId()).orElseThrow().getCurrentPrice();
        assertThat(persisted).isEqualByComparingTo(newPrice);
    }

    @DisplayName("updateCurrentPrice returns 0 and does not create a row for an unknown stock id")
    @Test
    void updateCurrentPriceUnknownId() {
        long unknownId = 999_999L;

        int affected = stockRepository.updateCurrentPrice(unknownId, new BigDecimal("1.0000"));

        assertThat(affected).isZero();
        assertThat(stockRepository.findById(unknownId)).isNotPresent();
        assertThat(stockRepository.count()).isEqualTo(4);
    }
}
