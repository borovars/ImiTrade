package ImiTrade.account.domain;

import ImiTrade.account.dto.AccountResponse;
import ImiTrade.portfolio.domain.PortfolioPosition;
import ImiTrade.portfolio.domain.PortfolioPositionRepository;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserService;
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
 * Read-only application service for the current user's account summary.
 *
 * <p>It aggregates the persisted {@link User} balance with a live view of the user's
 * {@link PortfolioPosition}s to produce the {@code GET /api/v1/account} snapshot used
 * as the application's main screen. This service never writes data: it only reads
 * the user, enriches each position with its live {@link Stock} price, and computes
 * the portfolio aggregates in memory.
 *
 * <p>Computed fields (never persisted):
 * <pre>{@code
 *   portfolioValue = Σ(currentPrice × quantity)
 *   profitLoss     = Σ((currentPrice − averagePrice) × quantity)
 *   totalAssets    = balance + portfolioValue
 * }</pre>
 * All monetary computations use the project-wide money scale (4, {@link RoundingMode#HALF_UP}),
 * matching the weighted-average PnL computation in {@code PortfolioService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    /** Money scale used across the project (NUMERIC(19,4)). */
    private static final int MONEY_SCALE = 4;

    private final UserService userService;
    private final PortfolioPositionRepository portfolioPositionRepository;
    private final StockRepository stockRepository;

    /**
     * Returns the account summary of the given user.
     *
     * <p>An empty portfolio yields {@code portfolioValue = 0}, {@code profitLoss = 0},
     * {@code totalAssets = balance} and {@code positionsCount = 0}; the stock lookup is
     * skipped entirely in that case.
     *
     * @param userId the authenticated user id
     * @return the account summary (never {@code null})
     */
    @Transactional(readOnly = true)
    public AccountResponse getCurrentAccount(Long userId) {
        User user = userService.getById(userId);

        List<PortfolioPosition> positions = portfolioPositionRepository.findByUserId(userId);
        if (positions.isEmpty()) {
            log.debug("Account summary for user={} has no positions", userId);
            BigDecimal balance = user.getBalance();
            BigDecimal portfolioValue = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal profitLoss = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            return AccountResponse.of(user, portfolioValue, balance, profitLoss, 0);
        }

        Map<Long, Stock> stocksById = loadStocks(positions);

        BigDecimal portfolioValue = BigDecimal.ZERO;
        BigDecimal profitLoss = BigDecimal.ZERO;
        for (PortfolioPosition position : positions) {
            Stock stock = stocksById.get(position.getStockId());
            BigDecimal quantity = BigDecimal.valueOf(position.getQuantity());
            BigDecimal currentPrice = stock.getCurrentPrice();

            portfolioValue = portfolioValue.add(currentPrice.multiply(quantity));
            profitLoss = profitLoss.add(currentPrice.subtract(position.getAveragePrice()).multiply(quantity));
        }

        portfolioValue = portfolioValue.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        profitLoss = profitLoss.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalAssets = user.getBalance().add(portfolioValue).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        log.debug("Account summary for user={}: portfolioValue={}, profitLoss={}, positionsCount={}",
                userId, portfolioValue, profitLoss, positions.size());
        return AccountResponse.of(user, portfolioValue, totalAssets, profitLoss, positions.size());
    }

    /** Batch-loads all referenced stocks to avoid an N+1 lookup per position. */
    private Map<Long, Stock> loadStocks(List<PortfolioPosition> positions) {
        Set<Long> stockIds = positions.stream().map(PortfolioPosition::getStockId).collect(Collectors.toSet());
        return stockRepository.findAllById(stockIds).stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));
    }
}
