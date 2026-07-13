package ImiTrade.stocks.service;

import ImiTrade.stocks.dto.CandleResponse;
import ImiTrade.stocks.integration.moex.MoexHistoryClient;
import ImiTrade.stocks.integration.moex.MoexHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only application service that assembles a stock's price history.
 *
 * <p>For a given {@link HistoryPeriod} it computes the date range (lookback from
 * today), fetches raw candle rows from {@link MoexHistoryClient}, and maps them to
 * {@link CandleResponse} DTOs via {@link MoexHistoryMapper}. No history is persisted —
 * MOEX is the single source of truth, the catalog's persisted {@code current_price} is
 * untouched, and this service never writes.
 *
 * <p>The caller (controller) is expected to have already validated that the ticker
 * exists in the catalog, so only valid catalog tickers reach MOEX.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockHistoryService {

    private final MoexHistoryClient moexHistoryClient;
    private final MoexHistoryMapper moexHistoryMapper;
    private final Clock clock;

    /**
     * Returns the OHLCV candle history for the given ticker and period.
     *
     * <p>Convenience overload with {@code customFrom = null} — uses the period's
     * default lookback from today.
     *
     * @param ticker MOEX security identifier, e.g. {@code SBER}
     * @param period requested period; never {@code null}
     * @return candles sorted by time ascending; empty when MOEX returned no data for
     *         the range (weekends, holidays, brand-new listings)
     */
    @Transactional(readOnly = true)
    public List<CandleResponse> getHistory(String ticker, HistoryPeriod period) {
        return getHistory(ticker, period, null);
    }

    /**
     * Returns the OHLCV candle history for the given ticker and period, optionally
     * starting from a custom date (used for incremental scroll-to-past on the chart).
     *
     * <p>The candle {@code interval} (bucket size) always comes from the
     * {@code period} — it never changes regardless of {@code customFrom}. Only the
     * range start shifts: when {@code customFrom} is {@code null}, the start is
     * computed from the period's lookback ({@code period.from(today)}); when it is
     * provided, the start is exactly {@code customFrom}. {@code till} is always
     * {@code today}. MOEX itself returns only the rows that exist for the range, so
     * an earlier {@code customFrom} simply yields more candles of the same interval.
     *
     * @param ticker     MOEX security identifier, e.g. {@code SBER}
     * @param period     requested period (provides the candle interval); never {@code null}
     * @param customFrom optional range start; {@code null} → period's default lookback
     * @return candles sorted by time ascending; empty when MOEX returned no data for
     *         the range (weekends, holidays, brand-new listings)
     */
    @Transactional(readOnly = true)
    public List<CandleResponse> getHistory(String ticker, HistoryPeriod period, LocalDate customFrom) {
        LocalDate till = LocalDate.now(clock);
        LocalDate from = customFrom != null ? customFrom : period.from(till);
        log.debug("Fetching history: ticker={} period={} from={} till={} interval={} customFrom={}",
                ticker, period.code(), from, till, period.moexInterval(), customFrom);
        var rows = moexHistoryClient.getCandles(ticker, from, till, period.moexInterval());
        return moexHistoryMapper.toCandles(rows);
    }
}
