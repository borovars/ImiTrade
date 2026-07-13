package ImiTrade.stocks.integration.moex.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import java.math.BigDecimal;

/**
 * A single {@code candles} row for one time bucket of a security.
 *
 * <p>MOEX returns each row as a positional JSON array. The column order is fixed by
 * the request ({@code candles.columns=begin,end,open,close,high,low,value,volume}),
 * so the record is declared in the same order and {@link JsonFormat} with
 * {@link Shape#ARRAY} tells Jackson to decode each element positionally — declarative,
 * no manual parsing, no {@code Map}. Mirrors the existing {@code MoexMarketDataRow}.
 *
 * @param begin  start of the candle bucket, e.g. {@code "2026-07-07 10:00:00"} (MSK)
 * @param end    end of the candle bucket, e.g. {@code "2026-07-07 10:10:00"} (MSK)
 * @param open   open price of the bucket; {@code null} when no trades happened
 * @param close  close price of the bucket; {@code null} when no trades happened
 * @param high   highest price of the bucket; {@code null} when no trades happened
 * @param low    lowest price of the bucket; {@code null} when no trades happened
 * @param value  total trade value in RUB; {@code null} when no trades happened
 * @param volume total traded volume (number of shares); {@code null} when no trades
 */
@JsonFormat(shape = Shape.ARRAY)
public record MoexCandleRow(
        String begin,
        String end,
        BigDecimal open,
        BigDecimal close,
        BigDecimal high,
        BigDecimal low,
        BigDecimal value,
        Long volume
) {
}
