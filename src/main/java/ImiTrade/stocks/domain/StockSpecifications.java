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

    /** Case-insensitive exact match on {@code ticker} (e.g. {@code ?ticker=SBER}). */
    public static Specification<Stock> hasTickerIgnoreCase(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("ticker")), ticker.toLowerCase());
    }

    /** Case-insensitive partial match on {@code company_name} (e.g. {@code ?companyName=Сбербанк}). */
    public static Specification<Stock> companyNameContainsIgnoreCase(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return null;
        }
        String pattern = "%" + companyName.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("companyName")), pattern);
    }

    /**
     * Combined search: case-insensitive partial match on {@code ticker}
     * <strong>OR</strong> {@code company_name}. Used by the catalog search field,
     * where the user types free text that may match either field (e.g. «sber» or
     * «Сбербанк»). Returns {@code null} for a blank argument.
     */
    public static Specification<Stock> tickerOrCompanyNameContainsIgnoreCase(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("ticker")), pattern),
                cb.like(cb.lower(root.get("companyName")), pattern));
    }
}
