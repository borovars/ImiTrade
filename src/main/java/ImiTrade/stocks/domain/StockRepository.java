package ImiTrade.stocks.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Access to {@link Stock} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} so the service layer can combine
 * dynamic filters (ticker, company name) with {@link org.springframework.data.domain.Pageable}.
 *
 * <p>The API surface is otherwise read-only; the only write path is
 * {@link #updateCurrentPrice(Long, BigDecimal)}, used exclusively by the market-data
 * scheduler to keep {@code stocks.current_price} in sync with MOEX. It is a targeted
 * UPDATE (no SELECT, no merge), so a failed refresh for one ticker never rolls back
 * the price update of another.
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, Long>, JpaSpecificationExecutor<Stock> {

    /**
     * Updates {@code current_price} for a single stock by id. Used by the market-data
     * scheduler; not intended for the public Stocks API. Runs in its own transaction so
     * that each refresh is committed independently — a failure for one ticker never
     * rolls back the price update of another.
     *
     * @param id    the stock id
     * @param price the new current price
     * @return the number of rows affected (1 if the stock exists, 0 otherwise)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Stock s SET s.currentPrice = :price WHERE s.id = :id")
    @Transactional
    int updateCurrentPrice(@Param("id") Long id, @Param("price") BigDecimal price);
}
