package ImiTrade.market.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import java.math.BigDecimal;

/**
 * A single {@code marketdata} row for one {@code (SECID, BOARDID)} pair.
 *
 * <p>MOEX returns each row as a positional JSON array (e.g.
 * {@code ["SBER", "TQBR", 312.45]}). The column order is fixed by the request
 * ({@code marketdata.columns=SECID,BOARDID,LAST}), so the record is declared in the
 * same order and {@link JsonFormat} with {@link Shape#ARRAY} tells Jackson to decode
 * each element positionally — declarative, no manual parsing, no {@code Map}.
 *
 * @param secid   security identifier (ticker), e.g. {@code SBER}
 * @param boardid trading board, e.g. {@code TQBR}
 * @param last    last traded price; {@code null} when no trade has happened yet
 */
@JsonFormat(shape = Shape.ARRAY)
public record MoexMarketDataRow(
        String secid,
        String boardid,
        BigDecimal last
) {
}
