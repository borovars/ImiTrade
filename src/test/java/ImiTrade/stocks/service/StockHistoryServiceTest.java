package ImiTrade.stocks.service;

import ImiTrade.stocks.dto.CandleResponse;
import ImiTrade.stocks.integration.moex.MoexHistoryClient;
import ImiTrade.stocks.integration.moex.MoexHistoryMapper;
import ImiTrade.stocks.integration.moex.dto.MoexCandleRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockHistoryServiceTest {

    /** Fixed clock at 2026-07-07T12:00:00Z so date math is deterministic. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-07T12:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private MoexHistoryClient moexHistoryClient;

    @Mock
    private MoexHistoryMapper moexHistoryMapper;

    private StockHistoryService stockHistoryService;

    private StockHistoryService service() {
        // Constructed explicitly so the fixed clock is injected (no @InjectMocks needed).
        stockHistoryService = new StockHistoryService(moexHistoryClient, moexHistoryMapper, FIXED_CLOCK);
        return stockHistoryService;
    }

    @Test
    @DisplayName("getHistory: delegates to the client with the 1D range and interval (daily, 3 months)")
    void getHistoryOneDay() {
        when(moexHistoryClient.getCandles(eq("SBER"),
                eq(LocalDate.of(2026, 4, 7)), eq(LocalDate.of(2026, 7, 7)), eq(24)))
                .thenReturn(List.of());
        when(moexHistoryMapper.toCandles(anyList())).thenReturn(List.of());

        service().getHistory("SBER", HistoryPeriod.D1);

        verify(moexHistoryClient).getCandles("SBER",
                LocalDate.of(2026, 4, 7), LocalDate.of(2026, 7, 7), 24);
    }

    @Test
    @DisplayName("getHistory: uses weekly candles for 1W (interval=7, 5 months)")
    void getHistoryOneWeekInterval() {
        when(moexHistoryClient.getCandles(eq("SBER"), any(LocalDate.class), any(LocalDate.class), eq(7)))
                .thenReturn(List.of());
        when(moexHistoryMapper.toCandles(anyList())).thenReturn(List.of());

        service().getHistory("SBER", HistoryPeriod.W1);

        verify(moexHistoryClient).getCandles("SBER",
                LocalDate.of(2026, 2, 7), LocalDate.of(2026, 7, 7), 7);
    }

    @Test
    @DisplayName("getHistory: uses monthly candles for 1M (interval=31, 3 years)")
    void getHistoryOneMonthInterval() {
        when(moexHistoryClient.getCandles(eq("SBER"), any(LocalDate.class), any(LocalDate.class), eq(31)))
                .thenReturn(List.of());
        when(moexHistoryMapper.toCandles(anyList())).thenReturn(List.of());

        service().getHistory("SBER", HistoryPeriod.M1);

        verify(moexHistoryClient).getCandles("SBER",
                LocalDate.of(2023, 7, 7), LocalDate.of(2026, 7, 7), 31);
    }

    @Test
    @DisplayName("getHistory: uses quarterly candles for 1Y (interval=4, 10 years)")
    void getHistoryOneYearInterval() {
        when(moexHistoryClient.getCandles(eq("SBER"), any(LocalDate.class), any(LocalDate.class), eq(4)))
                .thenReturn(List.of());
        when(moexHistoryMapper.toCandles(anyList())).thenReturn(List.of());

        service().getHistory("SBER", HistoryPeriod.Y1);

        verify(moexHistoryClient).getCandles("SBER",
                LocalDate.of(2016, 7, 7), LocalDate.of(2026, 7, 7), 4);
    }

    @Test
    @DisplayName("getHistory: returns the candles produced by the mapper, in order")
    void getHistoryReturnsMappedCandles() {
        var row = new MoexCandleRow("2026-07-07 10:00:00", "2026-07-07 10:10:00",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1L);
        var candle = new CandleResponse(Instant.parse("2026-07-07T07:00:00Z"),
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1L);
        when(moexHistoryClient.getCandles(any(String.class), any(LocalDate.class),
                any(LocalDate.class), anyInt()))
                .thenReturn(List.of(row));
        when(moexHistoryMapper.toCandles(anyList())).thenReturn(List.of(candle));

        List<CandleResponse> result = service().getHistory("SBER", HistoryPeriod.D1);

        assertThat(result).containsExactly(candle);
        verify(moexHistoryMapper).toCandles(List.of(row));
    }

    @Test
    @DisplayName("getHistory: returns empty list when MOEX has no data (holidays/weekends)")
    void getHistoryEmptyWhenMoexReturnsNothing() {
        when(moexHistoryClient.getCandles(any(String.class), any(LocalDate.class),
                any(LocalDate.class), anyInt()))
                .thenReturn(List.of());
        when(moexHistoryMapper.toCandles(anyList())).thenReturn(List.of());

        List<CandleResponse> result = service().getHistory("SBER", HistoryPeriod.D1);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getHistory(customFrom): shifts the range start to customFrom, keeps the period interval")
    void getHistoryWithCustomFrom() {
        when(moexHistoryClient.getCandles(eq("SBER"),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 7, 7)), eq(24)))
                .thenReturn(List.of());
        when(moexHistoryMapper.toCandles(anyList())).thenReturn(List.of());

        // 1D period (interval=24), но `from` уходит глубже в прошлое — для
        // инкрементальной догрузки графика влево. Интервал остаётся дневной.
        service().getHistory("SBER", HistoryPeriod.D1, LocalDate.of(2026, 1, 1));

        verify(moexHistoryClient).getCandles("SBER",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 7), 24);
    }

    @Test
    @DisplayName("getHistory(customFrom=null): behaves like the default-lookback overload")
    void getHistoryWithNullCustomFromFallsBackToDefault() {
        when(moexHistoryClient.getCandles(eq("SBER"),
                eq(LocalDate.of(2026, 4, 7)), eq(LocalDate.of(2026, 7, 7)), eq(24)))
                .thenReturn(List.of());
        when(moexHistoryMapper.toCandles(anyList())).thenReturn(List.of());

        service().getHistory("SBER", HistoryPeriod.D1, null);

        verify(moexHistoryClient).getCandles("SBER",
                LocalDate.of(2026, 4, 7), LocalDate.of(2026, 7, 7), 24);
    }
}
