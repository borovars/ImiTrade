package ImiTrade.stocks.integration.moex;

import ImiTrade.stocks.dto.CandleResponse;
import ImiTrade.stocks.integration.moex.dto.MoexCandleRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoexHistoryMapperTest {

    private final MoexHistoryMapper mapper = new MoexHistoryMapper();

    @DisplayName("maps rows to candles preserving OHLCV and converting MSK begin to UTC")
    @Test
    void mapsRowsToCandles() {
        // 10:00 MSK = 07:00 UTC
        MoexCandleRow row = new MoexCandleRow(
                "2026-07-07 10:00:00", "2026-07-07 10:10:00",
                new BigDecimal("312.40"), new BigDecimal("312.60"),
                new BigDecimal("312.80"), new BigDecimal("312.30"),
                new BigDecimal("1234567.80"), 3950L);

        List<CandleResponse> result = mapper.toCandles(List.of(row));

        assertThat(result).hasSize(1);
        CandleResponse candle = result.get(0);
        assertThat(candle.time()).isEqualTo(Instant.parse("2026-07-07T07:00:00Z"));
        assertThat(candle.open()).isEqualByComparingTo("312.40");
        assertThat(candle.high()).isEqualByComparingTo("312.80");
        assertThat(candle.low()).isEqualByComparingTo("312.30");
        assertThat(candle.close()).isEqualByComparingTo("312.60");
        assertThat(candle.volume()).isEqualTo(3950L);
    }

    @DisplayName("drops rows with blank begin or null OHLC (MOEX empty buckets)")
    @Test
    void dropsEmptyBuckets() {
        MoexCandleRow valid = new MoexCandleRow(
                "2026-07-07 10:00:00", "2026-07-07 10:10:00",
                new BigDecimal("1"), new BigDecimal("2"),
                new BigDecimal("3"), new BigDecimal("0.5"),
                new BigDecimal("100"), 10L);
        MoexCandleRow nullBegin = new MoexCandleRow(
                null, null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1L);
        MoexCandleRow blankBegin = new MoexCandleRow(
                "", "", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1L);
        MoexCandleRow allNullOhlc = new MoexCandleRow(
                "2026-07-07 10:20:00", "2026-07-07 10:30:00",
                null, null, null, null, BigDecimal.ZERO, 0L);

        List<CandleResponse> result = mapper.toCandles(List.of(valid, nullBegin, blankBegin, allNullOhlc));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).time()).isEqualTo(Instant.parse("2026-07-07T07:00:00Z"));
    }

    @DisplayName("sorts candles ascending by time regardless of input order")
    @Test
    void sortsByTimeAscending() {
        MoexCandleRow later = new MoexCandleRow(
                "2026-07-07 11:00:00", "2026-07-07 11:10:00",
                new BigDecimal("2"), new BigDecimal("2"), new BigDecimal("2"), new BigDecimal("2"),
                new BigDecimal("10"), 2L);
        MoexCandleRow earlier = new MoexCandleRow(
                "2026-07-07 10:00:00", "2026-07-07 10:10:00",
                new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("1"),
                new BigDecimal("10"), 1L);

        List<CandleResponse> result = mapper.toCandles(List.of(later, earlier));

        assertThat(result).extracting(CandleResponse::time)
                .containsExactly(
                        Instant.parse("2026-07-07T07:00:00Z"),
                        Instant.parse("2026-07-07T08:00:00Z"));
    }

    @DisplayName("returns empty list for null/empty input")
    @Test
    void returnsEmptyForNullOrEmpty() {
        assertThat(mapper.toCandles(null)).isEmpty();
        assertThat(mapper.toCandles(List.of())).isEmpty();
    }

    @DisplayName("skips rows with unparseable begin timestamps")
    @Test
    void skipsUnparseableTimestamp() {
        MoexCandleRow bad = new MoexCandleRow(
                "not-a-date", null,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1L);
        MoexCandleRow good = new MoexCandleRow(
                "2026-07-07 10:00:00", "2026-07-07 10:10:00",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1L);

        List<CandleResponse> result = mapper.toCandles(List.of(bad, good));

        assertThat(result).hasSize(1);
    }

    @DisplayName("MOEX timestamps are treated as UTC+3 regardless of system timezone")
    @Test
    void moexTimestampsUseFixedOffset() {
        // Midnight MSK = 21:00 UTC the previous day. This must hold even if the JVM
        // default timezone is not Moscow.
        MoexCandleRow midnightMsk = new MoexCandleRow(
                "2026-07-07 00:00:00", "2026-07-07 00:10:00",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1L);

        List<CandleResponse> result = mapper.toCandles(List.of(midnightMsk));

        assertThat(result.get(0).time())
                .as("midnight MSK is 21:00 UTC the previous day")
                .isEqualTo(Instant.parse("2026-07-06T21:00:00Z"));
    }
}
