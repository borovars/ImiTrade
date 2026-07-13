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
 * <p>The API surface is otherwise read-only; the only write paths are
 * {@link #updateCurrentPrice(Long, BigDecimal)} and {@link #updateLotSize(Long, Integer)},
 * used exclusively by the market-data scheduler to keep {@code stocks.current_price} and
 * {@code stocks.lot_size} in sync with MOEX. They are targeted UPDATEs (no SELECT, no
 * merge), so a failed refresh for one ticker never rolls back the update of another.
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

    /**
     * Updates {@code lot_size} for a single stock by id. Used by the market-data
     * scheduler; not intended for the public Stocks API. Runs in its own transaction so
     * that each refresh is committed independently — a failure for one ticker never
     * rolls back the lot-size update of another.
     *
     * @param id      the stock id
     * @param lotSize the new lot size
     * @return the number of rows affected (1 if the stock exists, 0 otherwise)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Stock s SET s.lotSize = :lotSize WHERE s.id = :id")
    @Transactional
    int updateLotSize(@Param("id") Long id, @Param("lotSize") Integer lotSize);
}
