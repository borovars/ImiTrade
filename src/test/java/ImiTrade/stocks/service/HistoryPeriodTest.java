package ImiTrade.stocks.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HistoryPeriodTest {

    @ParameterizedTest(name = "parse(\"{0}\") returns {1}")
    @DisplayName("parse: maps wire codes to periods (case-insensitive)")
    @CsvSource({
            "1D, D1",
            "1W, W1",
            "1M, M1",
            "1Y, Y1",
            "1d, D1",
            " 1d , D1"
    })
    void parseAcceptsValidCodes(String input, HistoryPeriod expected) {
        assertThat(HistoryPeriod.parse(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("parse: null/blank defaults to 1D")
    void parseDefaultsTo1D() {
        assertThat(HistoryPeriod.parse(null)).isEqualTo(HistoryPeriod.D1);
        assertThat(HistoryPeriod.parse("")).isEqualTo(HistoryPeriod.D1);
        assertThat(HistoryPeriod.parse("   ")).isEqualTo(HistoryPeriod.D1);
    }

    @Test
    @DisplayName("parse: unknown code throws (incl. legacy 3M which is no longer supported)")
    void parseRejectsUnknownCode() {
        assertThatThrownBy(() -> HistoryPeriod.parse("2D"))
                .isInstanceOf(IllegalArgumentException.class);
        // 3M was removed in the T-Investments-style model (1W/1M cover these ranges).
        assertThatThrownBy(() -> HistoryPeriod.parse("3M"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "{0} lookback from 2026-07-07 is {1}, interval {2}")
    @DisplayName("from/interval: each period computes the right lookback and interval")
    @CsvSource({
            "D1, 2026-04-07, 24",
            "W1, 2026-02-07, 7",
            "M1, 2023-07-07, 31",
            "Y1, 2016-07-07, 4"
    })
    void lookbackAndInterval(HistoryPeriod period, LocalDate expectedFrom, int expectedInterval) {
        LocalDate today = LocalDate.of(2026, 7, 7);

        assertThat(period.from(today)).isEqualTo(expectedFrom);
        assertThat(period.moexInterval()).isEqualTo(expectedInterval);
    }
}
