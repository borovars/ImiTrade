package ImiTrade.trading.domain;

import ImiTrade.common.exception.InsufficientBalanceException;
import ImiTrade.common.exception.InsufficientStockQuantityException;
import ImiTrade.common.exception.InvalidQuantityException;
import ImiTrade.common.exception.PortfolioPositionNotFoundException;
import ImiTrade.portfolio.domain.PortfolioPosition;
import ImiTrade.portfolio.domain.PortfolioPositionRepository;
import ImiTrade.stocks.domain.Stock;
import ImiTrade.stocks.domain.StockService;
import ImiTrade.transaction.domain.Transaction;
import ImiTrade.transaction.domain.TransactionRepository;
import ImiTrade.transaction.domain.TransactionType;
import ImiTrade.trading.dto.BuyStockRequest;
import ImiTrade.trading.dto.SellStockRequest;
import ImiTrade.trading.dto.TradeResponse;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Application service for buy/sell trades.
 *
 * <p>Each operation is fully atomic: it appends a {@link Transaction} (the source
 * of truth), updates the aggregated {@link PortfolioPosition} and adjusts the
 * user's balance — all within a single {@link Transactional} boundary. Money math
 * always uses {@link BigDecimal} (never {@code double}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final UserService userService;
    private final StockService stockService;
    private final PortfolioPositionRepository portfolioPositionRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Buys shares of a stock at its current price for the given user.
     *
     * @param userId  the authenticated user id
     * @param request stock id and quantity
     * @return the recorded trade
     * @throws InvalidQuantityException    if {@code quantity <= 0}
     * @throws StockNotFoundException      if the stock does not exist
     * @throws InsufficientBalanceException if the user cannot cover the trade total
     */
    @Transactional
    public TradeResponse buy(Long userId, BuyStockRequest request) {
        int quantity = request.quantity();
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }

        User user = userService.getById(userId);
        Stock stock = stockService.getStockById(request.stockId());
        BigDecimal price = stock.getCurrentPrice();
        BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(quantity));

        if (user.getBalance().compareTo(totalAmount) < 0) {
            throw new InsufficientBalanceException(user.getBalance(), totalAmount);
        }

        Transaction tx = transactionRepository.save(Transaction.builder()
                .userId(user.getId())
                .stockId(stock.getId())
                .type(TransactionType.BUY)
                .quantity(quantity)
                .price(price)
                .totalAmount(totalAmount)
                .createdAt(Instant.now())
                .build());

        PortfolioPosition position = portfolioPositionRepository
                .findByUserIdAndStockId(userId, stock.getId())
                .map(existing -> {
                    BigDecimal oldQty = BigDecimal.valueOf(existing.getQuantity());
                    BigDecimal buyQty = BigDecimal.valueOf(quantity);
                    BigDecimal newAverage = oldQty.multiply(existing.getAveragePrice())
                            .add(buyQty.multiply(price))
                            .divide(oldQty.add(buyQty), 4, RoundingMode.HALF_UP);
                    existing.setQuantity(existing.getQuantity() + quantity);
                    existing.setAveragePrice(newAverage);
                    return existing;
                })
                .orElseGet(() -> PortfolioPosition.builder()
                        .userId(userId)
                        .stockId(stock.getId())
                        .quantity(quantity)
                        .averagePrice(price)
                        .build());
        portfolioPositionRepository.save(position);

        user.setBalance(user.getBalance().subtract(totalAmount));

        log.info("BUY: user={} stock={} qty={} total={}", userId, stock.getTicker(), quantity, totalAmount);
        return TradeResponse.of(tx, stock.getTicker());
    }

    /**
     * Sells shares of a stock from the given user's portfolio at its current price.
     *
     * @param userId  the authenticated user id
     * @param request stock id and quantity
     * @return the recorded trade
     * @throws InvalidQuantityException           if {@code quantity <= 0}
     * @throws PortfolioPositionNotFoundException if the user holds no position for the stock
     * @throws InsufficientStockQuantityException if the position has fewer shares than requested
     */
    @Transactional
    public TradeResponse sell(Long userId, SellStockRequest request) {
        int quantity = request.quantity();
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }

        User user = userService.getById(userId);

        PortfolioPosition position = portfolioPositionRepository
                .findByUserIdAndStockId(userId, request.stockId())
                .orElseThrow(() -> new PortfolioPositionNotFoundException(userId, request.stockId()));

        if (position.getQuantity() < quantity) {
            throw new InsufficientStockQuantityException(position.getQuantity(), quantity);
        }

        Stock stock = stockService.getStockById(request.stockId());
        BigDecimal price = stock.getCurrentPrice();
        BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(quantity));

        Transaction tx = transactionRepository.save(Transaction.builder()
                .userId(user.getId())
                .stockId(stock.getId())
                .type(TransactionType.SELL)
                .quantity(quantity)
                .price(price)
                .totalAmount(totalAmount)
                .createdAt(Instant.now())
                .build());

        int remaining = position.getQuantity() - quantity;
        if (remaining == 0) {
            portfolioPositionRepository.delete(position);
        } else {
            position.setQuantity(remaining);
            portfolioPositionRepository.save(position);
        }

        user.setBalance(user.getBalance().add(totalAmount));

        log.info("SELL: user={} stock={} qty={} total={}", userId, stock.getTicker(), quantity, totalAmount);
        return TradeResponse.of(tx, stock.getTicker());
    }
}
