package ImiTrade.portfolio.domain;

import ImiTrade.portfolio.dto.PortfolioHistoryResponse;
import ImiTrade.portfolio.dto.PortfolioResponse;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import ImiTrade.stocks.dto.CandleResponse;
import ImiTrade.stocks.service.HistoryPeriod;
import ImiTrade.stocks.service.StockHistoryService;
import ImiTrade.transaction.domain.Transaction;
import ImiTrade.transaction.domain.TransactionRepository;
import ImiTrade.transaction.domain.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
 *
 * <p>The {@link #getHistory(Long, HistoryPeriod, LocalDate)} method reconstructs the
 * portfolio value time series by replaying the user's transactions against
 * historical MOEX candles fetched through {@link StockHistoryService} (the single
 * existing market-history source — no second MOEX integration). The reconstructed
 * value mirrors the live {@code portfolioValue} aggregate of {@code AccountService}
 * ({@code Σ quantity × closePrice}) but evaluated at each candle timestamp. Cash
 * balance is intentionally excluded — this is the market value of the investment
 * portfolio only. Nothing is persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    /** Money scale used across the project (NUMERIC(19,4)). */
    private static final int MONEY_SCALE = 4;

    private final PortfolioPositionRepository portfolioPositionRepository;
    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;
    private final StockHistoryService stockHistoryService;
    private final Clock clock;

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

    /**
     * Reconstructs the historical market value of the user's investment portfolio
     * as a time series aligned with stock candle buckets.
     *
     * <p>Algorithm (spec §"Формирование данных"):
     * <ol>
     *   <li>load the user's transactions in chronological order — empty list means
     *       the user never owned anything, return {@code []} (empty portfolio);</li>
     *   <li>resolve the distinct traded stock ids to tickers in one batch;</li>
     *   <li>fetch each ticker's candle history through {@link StockHistoryService}
     *       <em>once</em> (local cache keyed by ticker — never re-query MOEX for
     *       the same ticker inside one reconstruction, avoiding N×M calls);</li>
     *   <li>for every union timestamp, sum {@code quantity_held(stock, t) × close(stock, t)}
     *       across all stocks (forward-filled close, skipping stocks with no candle
     *       yet and skipping timestamps where the user held nothing, to avoid a flat
     *       zero line before the first buy / after a full sell-out).</li>
     * </ol>
     *
     * <p>Cash balance is excluded by design. MOEX/network failures propagate as
     * {@code MarketDataUnavailableException} (503) through {@code GlobalExceptionHandler}.
     *
     * @param userId the authenticated user id
     * @param period requested period (provides the candle interval); never {@code null}
     * @param from   optional range start ({@code null} → period's default lookback)
     * @return portfolio value points sorted by time ascending; empty when the user
     *         has never traded (never {@code null})
     */
    @Transactional(readOnly = true)
    public List<PortfolioHistoryResponse> getHistory(Long userId, HistoryPeriod period, LocalDate from) {
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtAsc(userId);
        if (transactions.isEmpty()) {
            log.debug("Portfolio history empty (no transactions) for user={}", userId);
            return List.of();
        }

        Map<Long, String> tickerByStockId = loadTickers(transactions);

        // One MOEX history call per distinct ticker — cached for the whole reconstruction.
        // ticker -> forward-filled close prices as of each candle timestamp (NavigableMap
        // for O(log n) "last close at or before t" via headMap(t, true).lastEntry()).
        Map<String, NavigableMap<Instant, BigDecimal>> closesByTicker = new HashMap<>();
        for (String ticker : new TreeSet<>(tickerByStockId.values())) {
            List<CandleResponse> candles = stockHistoryService.getHistory(ticker, period, from);
            NavigableMap<Instant, BigDecimal> closes = new TreeMap<>();
            for (CandleResponse candle : candles) {
                closes.put(candle.time(), candle.close());
            }
            closesByTicker.put(ticker, closes);
        }

        // Union of all candle timestamps — these are the evaluation points.
        TreeSet<Instant> timestamps = new TreeSet<>();
        for (NavigableMap<Instant, BigDecimal> closes : closesByTicker.values()) {
            timestamps.addAll(closes.keySet());
        }
        if (timestamps.isEmpty()) {
            log.debug("Portfolio history empty (no candles) for user={}", userId);
            return List.of();
        }

        // Per-stock quantity timeline: the share count held after each of the stock's
        // transactions, keyed by transaction time. Evaluated with headMap(t, true).
        Map<Long, NavigableMap<Instant, Integer>> quantityTimelines = buildQuantityTimelines(transactions);

        List<PortfolioHistoryResponse> result = new ArrayList<>(timestamps.size());
        for (Instant t : timestamps) {
            BigDecimal value = BigDecimal.ZERO;
            for (Map.Entry<Long, NavigableMap<Instant, Integer>> entry : quantityTimelines.entrySet()) {
                Integer qty = qtyAt(entry.getValue(), t);
                if (qty == null || qty <= 0) {
                    continue;
                }
                String ticker = tickerByStockId.get(entry.getKey());
                BigDecimal close = closeAt(closesByTicker.get(ticker), t);
                if (close == null) {
                    continue;
                }
                value = value.add(close.multiply(BigDecimal.valueOf(qty)));
            }
            // Skip points where the user held nothing (before first buy / after full sell-out)
            // to avoid a misleading flat zero segment on the chart.
            if (value.signum() <= 0) {
                continue;
            }
            result.add(new PortfolioHistoryResponse(t, value.setScale(MONEY_SCALE, RoundingMode.HALF_UP)));
        }
        return result;
    }

    /** Batch-loads all referenced stocks to avoid an N+1 lookup per position. */
    private Map<Long, Stock> loadStocks(List<PortfolioPosition> positions) {
        Set<Long> stockIds = positions.stream().map(PortfolioPosition::getStockId).collect(Collectors.toSet());
        return stockRepository.findAllById(stockIds).stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));
    }

    /** Resolves distinct traded stock ids to their tickers in a single batch query. */
    private Map<Long, String> loadTickers(List<Transaction> transactions) {
        Set<Long> stockIds = transactions.stream().map(Transaction::getStockId).collect(Collectors.toSet());
        return stockRepository.findAllById(stockIds).stream()
                .collect(Collectors.toMap(Stock::getId, Stock::getTicker));
    }

    /**
     * Builds, per stock id, a timeline of the cumulative share count held immediately
     * after each transaction (BUY adds, SELL subtracts). Used to answer
     * "how many shares of this stock did the user hold at time t?".
     */
    private Map<Long, NavigableMap<Instant, Integer>> buildQuantityTimelines(List<Transaction> transactions) {
        Map<Long, NavigableMap<Instant, Integer>> timelines = new HashMap<>();
        Map<Long, Integer> running = new HashMap<>();
        for (Transaction tx : transactions) {
            int delta = tx.getType() == TransactionType.BUY ? tx.getQuantity() : -tx.getQuantity();
            int updated = running.getOrDefault(tx.getStockId(), 0) + delta;
            running.put(tx.getStockId(), updated);
            timelines.computeIfAbsent(tx.getStockId(), k -> new TreeMap<>())
                    .put(tx.getCreatedAt(), updated);
        }
        return timelines;
    }

    /** Shares of {@code stockId} held at {@code t} (last known cumulative count at or before {@code t}). */
    private static Integer qtyAt(NavigableMap<Instant, Integer> timeline, Instant t) {
        Map.Entry<Instant, Integer> e = timeline.headMap(t, true).lastEntry();
        return e == null ? null : e.getValue();
    }

    /** Last known close price of the ticker at or before {@code t} (forward fill). */
    private static BigDecimal closeAt(NavigableMap<Instant, BigDecimal> closes, Instant t) {
        Map.Entry<Instant, BigDecimal> e = closes.headMap(t, true).lastEntry();
        return e == null ? null : e.getValue();
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
