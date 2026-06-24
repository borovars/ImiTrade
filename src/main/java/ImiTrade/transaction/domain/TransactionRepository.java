package ImiTrade.transaction.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Access to {@link Transaction} entities. Appends are write-once; the table is
 * the source of truth for the trading history.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
