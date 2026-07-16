package ImiTrade.transaction.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Access to {@link Transaction} entities. Appends are write-once; the table is
 * the source of truth for the trading history.
 *
 * <p>Extends {@link JpaSpecificationExecutor} so the read-side history service can
 * combine dynamic filters (userId, type, stockId) with
 * {@link org.springframework.data.domain.Pageable}, without filtering in memory.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /**
     * Returns a user's full trade history in chronological order, used to replay
     * past holdings when reconstructing the portfolio value time series. The
     * portfolio-history read path needs the complete, unpaginated stream; the
     * paginated {@code TransactionService.getTransactions} accessor does not fit.
     *
     * @param userId the owner of the transactions
     * @return transactions ordered by {@code createdAt} ascending (never {@code null})
     */
    List<Transaction> findByUserIdOrderByCreatedAtAsc(Long userId);
}
