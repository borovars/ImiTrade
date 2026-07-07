package ImiTrade.market.domain;

import ImiTrade.market.client.MoexClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Application service for live market data. Today it only delegates to
 * {@link MoexClient}, but this is the single home for future cross-cutting logic
 * (caching, retry, scheduler-driven preloading, fallback), so the client can stay
 * a dumb transport.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final MoexClient moexClient;

    /**
     * Returns the current price for the given ticker.
     *
     * @param ticker MOEX security identifier, e.g. {@code SBER}
     * @return the current price of the security
     * @throws ImiTrade.common.exception.InvalidTickerException        if the ticker is blank or unknown
     * @throws ImiTrade.common.exception.MarketDataUnavailableException if MOEX cannot serve the price
     */
    public BigDecimal getCurrentPrice(String ticker) {
        log.debug("Fetching current price for ticker={}", ticker);
        return moexClient.getCurrentPrice(ticker);
    }

    /**
     * Returns a snapshot (last price + lot size) for the given ticker, fetched in a
     * single MOEX ISS call.
     *
     * @param ticker MOEX security identifier, e.g. {@code SBER}
     * @return a {@link MoexClient.MoexSnapshot} with a non-null price and a nullable lot size
     * @throws ImiTrade.common.exception.InvalidTickerException        if the ticker is blank or unknown
     * @throws ImiTrade.common.exception.MarketDataUnavailableException if MOEX cannot serve the price
     */
    public MoexClient.MoexSnapshot getMarketSnapshot(String ticker) {
        log.debug("Fetching market snapshot for ticker={}", ticker);
        return moexClient.getMarketSnapshot(ticker);
    }
}
