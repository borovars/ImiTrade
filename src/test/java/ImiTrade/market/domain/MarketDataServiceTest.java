package ImiTrade.market.domain;

import ImiTrade.common.exception.InvalidTickerException;
import ImiTrade.common.exception.MarketDataUnavailableException;
import ImiTrade.market.client.MoexClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private MoexClient moexClient;

    @InjectMocks
    private MarketDataService marketDataService;

    @DisplayName("getCurrentPrice: delegates to MoexClient and returns its price")
    @Test
    void getCurrentPriceDelegatesToClient() {
        BigDecimal price = new BigDecimal("312.45");
        when(moexClient.getCurrentPrice("SBER")).thenReturn(price);

        BigDecimal result = marketDataService.getCurrentPrice("SBER");

        assertThat(result).isEqualByComparingTo(price);
        verify(moexClient).getCurrentPrice("SBER");
    }

    @DisplayName("getCurrentPrice: propagates InvalidTickerException from the client")
    @Test
    void getCurrentPricePropagatesInvalidTicker() {
        when(moexClient.getCurrentPrice("NOPE")).thenThrow(new InvalidTickerException("NOPE"));

        assertThatThrownBy(() -> marketDataService.getCurrentPrice("NOPE"))
                .isInstanceOf(InvalidTickerException.class);
    }

    @DisplayName("getCurrentPrice: propagates MarketDataUnavailableException from the client")
    @Test
    void getCurrentPricePropagatesMarketDataUnavailable() {
        when(moexClient.getCurrentPrice("SBER"))
                .thenThrow(new MarketDataUnavailableException("MOEX down"));

        assertThatThrownBy(() -> marketDataService.getCurrentPrice("SBER"))
                .isInstanceOf(MarketDataUnavailableException.class);
    }
}
