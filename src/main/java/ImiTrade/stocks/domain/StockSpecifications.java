package ImiTrade.stocks.domain;

import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable {@link Specification} predicates for {@link Stock} queries.
 *
 * <p>Each factory returns {@code null} for a blank argument, so they can be freely
 * combined with {@code where(...).and(...)} in the service without producing an
 * empty filter.
 */
public final class StockSpecifications {

    private StockSpecifications() {
    }

    /** Case-insensitive exact match on {@code ticker} (e.g. {@code ?ticker=AAPL}). */
    public static Specification<Stock> hasTickerIgnoreCase(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("ticker")), ticker.toLowerCase());
    }

    /** Case-insensitive partial match on {@code company_name} (e.g. {@code ?companyName=Apple}). */
    public static Specification<Stock> companyNameContainsIgnoreCase(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return null;
        }
        String pattern = "%" + companyName.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("companyName")), pattern);
    }
}
