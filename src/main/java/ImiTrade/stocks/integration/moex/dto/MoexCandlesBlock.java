package ImiTrade.stocks.integration.moex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * MOEX ISS {@code candles} block.
 *
 * <p>MOEX returns data in a columnar layout: {@code columns} names the fields,
 * {@code data} holds one row per time bucket. The column order is fixed by the request
 * ({@code candles.columns=begin,end,open,close,high,low,value,volume}), so each row is
 * decoded positionally into {@link MoexCandleRow}. Mirrors {@code MoexMarketDataBlock}.
 *
 * @param columns field names returned by MOEX (kept for debugging/future use)
 * @param data    parsed candle rows; empty when MOEX has no data for the range
 */
public record MoexCandlesBlock(
        @JsonProperty("columns") List<String> columns,
        @JsonProperty("data") List<MoexCandleRow> data
) {
}
