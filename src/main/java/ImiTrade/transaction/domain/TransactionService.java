package ImiTrade.transaction.domain;

import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockRepository;
import ImiTrade.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only application service for the current user's transaction history.
 *
 * <p>It is a pure view over the persisted {@link Transaction} rows (which are
 * written exclusively by {@code TradeService}). This service never writes to
 * {@code transactions}: it filters the history by user / type / stock entirely on
 * the database side via {@link TransactionSpecifications} and enriches each row
 * with the referenced stock's ticker.
 *
 * <p>The {@code userId} is supplied by the controller from the authenticated
 * principal and is always part of the {@link Specification}, so a user can never
 * read another user's transactions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final StockRepository stockRepository;

    /**
     * Returns a page of the given user's transactions, optionally filtered by
     * operation type and/or stock.
     *
     * @param userId   the authenticated user id (always applied)
     * @param type     optional {@link TransactionType} filter
     * @param stockId  optional stock id filter
     * @param pageable paging and sorting
     * @return a page of {@link TransactionResponse} DTOs (never exposes entities)
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(Long userId, TransactionType type, Long stockId, Pageable pageable) {
        Specification<Transaction> spec = Specification.allOf(
                TransactionSpecifications.hasUserId(userId),
                TransactionSpecifications.hasType(type),
                TransactionSpecifications.hasStockId(stockId));

        Page<Transaction> page = transactionRepository.findAll(spec, pageable);
        if (page.isEmpty()) {
            log.debug("Transaction history empty for user={} type={} stockId={}", userId, type, stockId);
            return page.map(tx -> TransactionResponse.from(tx, null));
        }

        Map<Long, String> tickersById = loadTickers(page);
        return page.map(tx -> TransactionResponse.from(tx, tickersById.get(tx.getStockId())));
    }

    /** Batch-loads the tickers of all stocks referenced on the page to avoid an N+1 lookup. */
    private Map<Long, String> loadTickers(Page<Transaction> page) {
        Set<Long> stockIds = page.stream().map(Transaction::getStockId).collect(Collectors.toSet());
        return stockRepository.findAllById(stockIds).stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getTicker()));
    }
}
