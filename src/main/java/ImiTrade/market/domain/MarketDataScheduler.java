package ImiTrade.market.domain;

import ImiTrade.market.config.SchedulerProperties;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Periodically refreshes {@code stocks.current_price} from MOEX.
 *
 * <p>This is the <em>single</em> component that mutates {@code current_price}. Every
 * business module (Stocks/Trading/Portfolio/Account) reads the price from the database,
 * so once this job persists a fresh value, those modules automatically operate on it
 * without any change to their own code.
 *
 * <p>On each run it loads all stocks, fetches the live price for each ticker via
 * {@link MarketDataService} and writes it back with
 * {@link StockRepository#updateCurrentPrice(Long, BigDecimal)}. Each ticker is refreshed
 * independently: a failure for one ticker (MOEX unreachable, unknown ticker, etc.) is
 * logged and skipped and never aborts the run or rolls back already-applied updates.
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
     * Refreshes current prices for all stocks. Runs on the fixed rate configured under
     * {@code app.market.scheduler.fixed-rate} (ms, default {@code 60000}).
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
                BigDecimal price = marketDataService.getCurrentPrice(ticker);
                stockRepository.updateCurrentPrice(stock.getId(), price);
                log.debug("Refreshed price for {}: {}", ticker, price);
                updated++;
            } catch (RuntimeException ex) {
                errors++;
                log.warn("Failed to refresh price for {}: {}", ticker, ex.getMessage());
            }
        }

        log.info("Stock price refresh complete: updated={}, errors={}", updated, errors);
    }
}
