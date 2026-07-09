package ImiTrade.stocks.integration.moex;

import ImiTrade.stocks.dto.CandleResponse;
import ImiTrade.stocks.integration.moex.dto.MoexCandleRow;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Maps raw MOEX candle rows to public {@link CandleResponse} DTOs.
 *
 * <p>Keeps the MOEX-specific row shape out of the API response: it parses the MSK
 * {@code begin} timestamp to a UTC {@link Instant}, drops buckets with no trades
 * (null OHLC) and null {@code begin}, and returns the candles sorted ascending by
 * time so the frontend always receives a stable order regardless of how MOEX
 * returned them.
 *
 * <p>MOEX timestamps are Moscow time ({@code UTC+3}, no DST), so the parser uses a
 * fixed {@code +03:00} offset — independent of the server's default time zone.
 */
@Component
public class MoexHistoryMapper {

    /**
     * MOEX {@code candles.begin}/{@code end} format, e.g. {@code "2026-07-07 10:00:00"}.
     */
    private static final DateTimeFormatter MOEX_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Moscow Exchange time zone: fixed {@code UTC+3}, no DST.
     */
    private static final ZoneOffset MOEX_OFFSET = ZoneOffset.ofHours(3);

    /**
     * Converts raw MOEX rows to {@link CandleResponse}s.
     *
     * <p>Rows with a blank {@code begin} or a null OHLC set are skipped — MOEX emits
     * empty buckets for intervals with no trades, and those carry no useful
     * information for the chart. The result is sorted by {@code time} ascending.
     *
     * @param rows raw MOEX candle rows (may be empty, may be in any order)
     * @return clean OHLCV candles sorted by time ascending; never {@code null}
     */
    public List<CandleResponse> toCandles(List<MoexCandleRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .filter(row -> row.begin() != null && !row.begin().isBlank())
                .filter(row -> row.open() != null || row.close() != null
                        || row.high() != null || row.low() != null)
                .map(this::toCandle)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CandleResponse::time))
                .toList();
    }

    private CandleResponse toCandle(MoexCandleRow row) {
        Instant time = parseInstant(row.begin());
        if (time == null) {
            return null;
        }
        return new CandleResponse(
                time,
                row.open(),
                row.high(),
                row.low(),
                row.close(),
                row.volume()
        );
    }

    private Instant parseInstant(String value) {
        try {
            return LocalDateTime.parse(value, MOEX_TIMESTAMP).toInstant(MOEX_OFFSET);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
