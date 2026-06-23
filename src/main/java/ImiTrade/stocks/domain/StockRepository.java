package ImiTrade.stocks.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Read-only access to {@link Stock} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} so the service layer can combine
 * dynamic filters (ticker, company name) with {@link org.springframework.data.domain.Pageable}.
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, Long>, JpaSpecificationExecutor<Stock> {
}
