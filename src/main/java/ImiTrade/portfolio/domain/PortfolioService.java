package ImiTrade.portfolio.domain;

import ImiTrade.portfolio.dto.PortfolioResponse;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only application service for the current user's portfolio.
 *
 * <p>It is a pure view over the persisted {@link PortfolioPosition} rows (which are
 * maintained exclusively by {@code TradeService}). This service never writes to
 * {@code portfolio_positions}: it only enriches each position with its live
 * {@link Stock} snapshot and computes the unrealized {@code pnl} in memory.
 *
 * <p>{@code pnl} follows the formula from the specification:
 * <pre>{@code
 *   pnl = (currentPrice - averagePrice) * quantity
 * }</pre>
 * It is rounded to the project-wide money scale (4, {@link RoundingMode#HALF_UP}),
 * matching the weighted-average computation in {@code TradeService.buy}, and is
 * never persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    /** Money scale used across the project (NUMERIC(19,4)). */
    private static final int MONEY_SCALE = 4;

    private final PortfolioPositionRepository portfolioPositionRepository;
    private final StockRepository stockRepository;

    /**
     * Returns the current portfolio of the given user as a list of enriched,
     * read-only responses. An empty list is returned for a user with no holdings.
     *
     * @param userId the authenticated user id
     * @return portfolio positions with live prices and computed pnl (never {@code null})
     */
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getPortfolio(Long userId) {
        List<PortfolioPosition> positions = portfolioPositionRepository.findByUserId(userId);
        if (positions.isEmpty()) {
            log.debug("Portfolio empty for user={}", userId);
            return List.of();
        }

        Map<Long, Stock> stocksById = loadStocks(positions);

        return positions.stream()
                .map(position -> {
                    Stock stock = stocksById.get(position.getStockId());
                    BigDecimal pnl = unrealizedPnl(stock.getCurrentPrice(), position);
                    return PortfolioResponse.from(position, stock, pnl);
                })
                .toList();
    }

    /** Batch-loads all referenced stocks to avoid an N+1 lookup per position. */
    private Map<Long, Stock> loadStocks(List<PortfolioPosition> positions) {
        Set<Long> stockIds = positions.stream().map(PortfolioPosition::getStockId).collect(Collectors.toSet());
        return stockRepository.findAllById(stockIds).stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));
    }

    /**
     * Computes the unrealized profit/loss of a position against the current price.
     *
     * @param currentPrice the stock's live price
     * @param position     the holding
     * @return {@code (currentPrice - averagePrice) * quantity}, scale 4, HALF_UP
     */
    private BigDecimal unrealizedPnl(BigDecimal currentPrice, PortfolioPosition position) {
        return currentPrice.subtract(position.getAveragePrice())
                .multiply(BigDecimal.valueOf(position.getQuantity()))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
