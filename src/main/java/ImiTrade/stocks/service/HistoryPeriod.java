package ImiTrade.stocks.service;

import java.time.LocalDate;
import java.time.Period;

/**
 * A historical price-history period exposed by the {@code /stocks/{ticker}/history}
 * endpoint. Each value carries its MOEX {@code interval} (candle bucket size) and the
 * lookback applied backwards from {@code today}, so the date-range / interval mapping
 * lives in one place.
 *
 * <p>The wire format is the short {@code 1D/1W/1M/1Y} codes used by the frontend
 * period switch; the Java identifier is a valid identifier derived from it.
 *
 * <p><b>Model (T-Investiments / MOEX style):</b> each button is both a candle
 * interval and a default lookback. The frontend keeps the interval when lazily
 * loading older data on pan/wheel, so the whole series is always a single
 * bucket size — no artifacts from mixing intervals in one series.
 *
 * <p>MOEX {@code interval} values: {@code 24} = 1 day, {@code 7} = 1 week,
 * {@code 31} = 1 month, {@code 4} = 1 quarter.
 */
public enum HistoryPeriod {
    /** Daily candles, lookback = 3 months. */
    D1("1D", 24, Period.ofMonths(3)),
    /** Weekly candles, lookback = 5 months. */
    W1("1W", 7, Period.ofMonths(5)),
    /** Monthly candles, lookback = 3 years. */
    M1("1M", 31, Period.ofYears(3)),
    /** Quarterly candles, lookback = 10 years. */
    Y1("1Y", 4, Period.ofYears(10));

    private final String code;
    private final int moexInterval;
    private final Period lookback;

    HistoryPeriod(String code, int moexInterval, Period lookback) {
        this.code = code;
        this.moexInterval = moexInterval;
        this.lookback = lookback;
    }

    /** The short code used on the wire ({@code 1D}, {@code 1W}, …). */
    public String code() {
        return code;
    }

    /** The MOEX ISS {@code interval} query parameter for this period. */
    public int moexInterval() {
        return moexInterval;
    }

    /** Inclusive start date of the range, computed backwards from {@code today}. */
    public LocalDate from(LocalDate today) {
        return today.minus(lookback);
    }

    /**
     * Parses the wire code into a {@link HistoryPeriod}.
     *
     * @param code wire code, case-insensitive; {@code null}/blank → {@link #D1}
     * @return the matching period, or {@link #D1} when {@code code} is blank
     * @throws IllegalArgumentException if the code is non-blank and unknown
     */
    public static HistoryPeriod parse(String code) {
        if (code == null || code.isBlank()) {
            return D1;
        }
        String normalized = code.trim().toUpperCase();
        for (HistoryPeriod period : values()) {
            if (period.code.equals(normalized)) {
                return period;
            }
        }
        throw new IllegalArgumentException("Unsupported period: " + code);
    }
}
