package ImiTrade.market.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import java.math.BigDecimal;

/**
 * A single {@code securities} row for one {@code (SECID, BOARDID)} pair.
 *
 * <p>MOEX returns each row as a positional JSON array (e.g.
 * {@code ["SBER", "TQBR", 10]}). The column order is fixed by the request
 * ({@code securities.columns=SECID,LOTSIZE}), so the record is declared in the
 * same order and {@link JsonFormat} with {@link Shape#ARRAY} tells Jackson to
 * decode each element positionally. The lot size is decoded as a
 * {@link BigDecimal} (MOEX emits a JSON number) and narrowed to an integer by
 * the client.
 *
 * @param secid   security identifier (ticker), e.g. {@code SBER}
 * @param boardid trading board, e.g. {@code TQBR}
 * @param lotsize number of shares in one lot; {@code null} when MOEX has none
 */
@JsonFormat(shape = Shape.ARRAY)
public record MoexSecuritiesRow(
        String secid,
        String boardid,
        BigDecimal lotsize
) {
}
