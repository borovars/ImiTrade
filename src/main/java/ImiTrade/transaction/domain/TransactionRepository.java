package ImiTrade.transaction.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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
}
