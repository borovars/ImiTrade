package ImiTrade.market.domain;

import ImiTrade.market.client.MoexClient;
import ImiTrade.market.config.SchedulerProperties;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically refreshes {@code stocks.current_price} and {@code stocks.lot_size} from MOEX.
 *
 * <p>This is the <em>single</em> component that mutates {@code current_price} and
 * {@code lot_size}. Every business module (Stocks/Trading/Portfolio/Account) reads these
 * values from the database, so once this job persists fresh values, those modules
 * automatically operate on them without any change to their own code.
 *
 * <p>On each run it loads all stocks, fetches a {@link MoexClient.MoexSnapshot snapshot}
 * (last price + lot size) for each ticker via {@link MarketDataService} in a single MOEX
 * call, and writes both back with {@link StockRepository#updateCurrentPrice(Long, BigDecimal)}
 * and {@link StockRepository#updateLotSize(Long, Integer)}. The lot size is only updated
 * when MOEX actually returns one; otherwise the persisted value is kept (the lots-feature
 * contract forbids making up a lot size). Each ticker is refreshed independently: a
 * failure for one ticker (MOEX unreachable, unknown ticker, etc.) is logged and skipped
 * and never aborts the run or rolls back already-applied updates.
 *
 * <p>Only registered when {@code app.market.scheduler.enabled} is {@code true} (the
 * default), so the job can be turned off entirely from configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.market.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataScheduler {

    private final MarketDataService marketDataService;
    private final StockRepository stockRepository;
    private final SchedulerProperties schedulerProperties;

    /**
     * Refreshes current prices and lot sizes for all stocks. Runs on the fixed rate
     * configured under {@code app.market.scheduler.fixed-rate} (ms, default {@code 60000}).
     */
    @Scheduled(fixedRateString = "${app.market.scheduler.fixed-rate:60000}")
    public void refreshPrices() {
        log.info("Starting stock price refresh (fixedRate={}ms)", schedulerProperties.fixedRate());

        List<Stock> stocks = stockRepository.findAll();
        log.info("Found {} stock(s) to refresh", stocks.size());
        if (stocks.isEmpty()) {
            log.info("No stocks to refresh; skipping run");
            return;
        }

        int updated = 0;
        int errors = 0;
        for (Stock stock : stocks) {
            String ticker = stock.getTicker();
            try {
                MoexClient.MoexSnapshot snapshot = marketDataService.getMarketSnapshot(ticker);
                stockRepository.updateCurrentPrice(stock.getId(), snapshot.last());
                Integer lotSize = snapshot.lotSize();
                if (lotSize != null) {
                    stockRepository.updateLotSize(stock.getId(), lotSize);
                }
                log.debug("Refreshed snapshot for {}: price={} lotSize={}", ticker,
                        snapshot.last(), lotSize);
                updated++;
            } catch (RuntimeException ex) {
                errors++;
                log.warn("Failed to refresh snapshot for {}: {}", ticker, ex.getMessage());
            }
        }

        log.info("Stock price refresh complete: updated={}, errors={}", updated, errors);
    }
}
