package ImiTrade.market.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * MOEX ISS {@code securities} block.
 *
 * <p>MOEX returns data in a columnar layout: {@code columns} names the fields,
 * {@code data} holds one row per {@code (SECID, BOARDID)} pair. The column order
 * is fixed by the request ({@code securities.columns=SECID,BOARDID,LOTSIZE}), so
 * each row is decoded positionally into {@link MoexSecuritiesRow}.
 *
 * @param columns field names returned by MOEX (kept for debugging/future use)
 * @param data    parsed securities rows; empty when MOEX has no data for the ticker
 */
public record MoexSecuritiesBlock(
        @JsonProperty("columns") List<String> columns,
        @JsonProperty("data") List<MoexSecuritiesRow> data
) {
}
