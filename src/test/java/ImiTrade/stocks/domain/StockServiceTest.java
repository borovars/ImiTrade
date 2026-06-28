package ImiTrade.stocks.domain;

import ImiTrade.common.exception.StockNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    @DisplayName("getStocks: delegates to repository with the assembled specification and pageable")
    @Test
    void getStocksReturnsPage() {
        Stock sber = Stock.builder().id(1L).ticker("SBER").companyName("Сбербанк").exchange("MOEX").build();
        Pageable pageable = PageRequest.of(0, 10);
        @SuppressWarnings("unchecked")
        Page<Stock> returned = new PageImpl<>(List.of(sber), pageable, 1);
        when(stockRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(returned);

        Page<Stock> result = stockService.getStocks("SBER", null, pageable);

        assertThat(result.getContent()).containsExactly(sber);
        verify(stockRepository).findAll(any(Specification.class), eq(pageable));
    }

    @DisplayName("getStocks: null filters still query through the repository")
    @Test
    void getStocksWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        @SuppressWarnings("unchecked")
        Page<Stock> returned = new PageImpl<>(List.of(), pageable, 0);
        when(stockRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(returned);

        Page<Stock> result = stockService.getStocks(null, null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(stockRepository).findAll(any(Specification.class), eq(pageable));
    }

    @DisplayName("getStockById: returns the stock when found")
    @Test
    void getStockByIdFound() {
        Stock sber = Stock.builder().id(1L).ticker("SBER").companyName("Сбербанк").exchange("MOEX").build();
        when(stockRepository.findById(1L)).thenReturn(Optional.of(sber));

        assertThat(stockService.getStockById(1L)).isSameAs(sber);
    }

    @DisplayName("getStockById: throws StockNotFoundException when missing")
    @Test
    void getStockByIdMissing() {
        when(stockRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.getStockById(99L))
                .isInstanceOf(StockNotFoundException.class);
    }
}
