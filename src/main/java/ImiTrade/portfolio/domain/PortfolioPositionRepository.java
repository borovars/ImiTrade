package ImiTrade.portfolio.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Access to {@link PortfolioPosition} entities. There is at most one position
 * per {@code (user_id, stock_id)} pair, enforced by the unique constraint
 * {@code uk_portfolio_positions_user_stock} in {@code V1__init_schema.sql}.
 */
@Repository
public interface PortfolioPositionRepository extends JpaRepository<PortfolioPosition, Long> {

    /** Finds the user's position for a given stock, if any. */
    Optional<PortfolioPosition> findByUserIdAndStockId(Long userId, Long stockId);

    /** Returns all current positions for the given user (read-only portfolio view). */
    List<PortfolioPosition> findByUserId(Long userId);
}
