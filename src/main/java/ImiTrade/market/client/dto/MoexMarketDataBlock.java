package ImiTrade.market.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * MOEX ISS {@code marketdata} block.
 *
 * <p>MOEX returns data in a columnar layout: {@code columns} names the fields,
 * {@code data} holds one row per {@code (SECID, BOARDID)} pair. The column order is
 * fixed by the request ({@code marketdata.columns=SECID,BOARDID,LAST}), so each row
 * is decoded positionally into {@link MoexMarketDataRow} via
 * {@link MoexMarketDataRow#of(List)}.
 *
 * @param columns field names returned by MOEX (kept for debugging/future use)
 * @param data    parsed market-data rows; empty when MOEX has no data for the ticker
 */
public record MoexMarketDataBlock(
        @JsonProperty("columns") List<String> columns,
        @JsonProperty("data") List<MoexMarketDataRow> data
) {
}
