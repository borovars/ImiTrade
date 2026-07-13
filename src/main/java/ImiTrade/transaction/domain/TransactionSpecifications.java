package ImiTrade.transaction.domain;

import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable {@link Specification} predicates for {@link Transaction} queries.
 *
 * <p>Each factory returns {@code null} for a missing argument, so they can be freely
 * combined with {@code Specification.allOf(...)} in the service without producing an
 * empty filter. The {@code userId} predicate is always applied by the service so a
 * user can only ever see their own transactions.
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    /** Exact match on {@code user_id} — isolates the result to a single user. */
    public static Specification<Transaction> hasUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    /** Exact match on {@code type} (e.g. {@code ?type=BUY}). */
    public static Specification<Transaction> hasType(TransactionType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    /** Exact match on {@code stock_id} (e.g. {@code ?stockId=1}). */
    public static Specification<Transaction> hasStockId(Long stockId) {
        if (stockId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("stockId"), stockId);
    }
}
