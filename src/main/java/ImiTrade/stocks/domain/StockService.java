package ImiTrade.stocks.domain;

import ImiTrade.common.exception.StockNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only application service for {@link Stock}.
 *
 * <p>Supports paged listing with optional {@code ticker} (case-insensitive exact)
 * and {@code companyName} (case-insensitive partial) filters, and lookup by id.
 * Persistence is delegated to {@link StockRepository}; filters are assembled with
 * {@link StockSpecifications}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    /**
     * Returns a page of stocks, optionally filtered by ticker and/or company name
     * and/or a combined search term.
     *
     * @param ticker      optional exact, case-insensitive ticker filter
     * @param companyName optional partial, case-insensitive company name filter
     * @param search      optional partial, case-insensitive search across ticker
     *                    AND company name (OR-combined). Used by the catalog
     *                    search field.
     * @param pageable    paging and sorting
     * @return a page of matching {@link Stock} entities
     */
    @Transactional(readOnly = true)
    public Page<Stock> getStocks(String ticker, String companyName, String search, Pageable pageable) {
        Specification<Stock> spec = Specification.allOf(
                StockSpecifications.hasTickerIgnoreCase(ticker),
                StockSpecifications.companyNameContainsIgnoreCase(companyName),
                StockSpecifications.tickerOrCompanyNameContainsIgnoreCase(search));
        return stockRepository.findAll(spec, pageable);
    }

    /**
     * Looks up a stock by id.
     *
     * @throws StockNotFoundException if no stock with the given id exists
     */
    @Transactional(readOnly = true)
    public Stock getStockById(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new StockNotFoundException(id));
    }

    /**
     * Looks up a stock by ticker (case-insensitive exact match).
     *
     * <p>Used to validate a path-variable ticker before resolving derived data
     * (e.g. price history) — the same case-insensitive semantics as the
     * {@code ?ticker=} list filter.
     *
     * @throws StockNotFoundException if no stock with the given ticker exists
     */
    @Transactional(readOnly = true)
    public Stock getStockByTicker(String ticker) {
        Specification<Stock> spec = StockSpecifications.hasTickerIgnoreCase(ticker);
        return stockRepository.findOne(spec)
                .orElseThrow(() -> new StockNotFoundException(ticker));
    }
}
