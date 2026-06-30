package ImiTrade.common.web;

import ImiTrade.common.exception.EmailAlreadyExistsException;
import ImiTrade.common.exception.InsufficientBalanceException;
import ImiTrade.common.exception.InsufficientStockQuantityException;
import ImiTrade.common.exception.InvalidCredentialsException;
import ImiTrade.common.exception.InvalidQuantityException;
import ImiTrade.common.exception.InvalidTickerException;
import ImiTrade.common.exception.MarketDataUnavailableException;
import ImiTrade.common.exception.PortfolioPositionNotFoundException;
import ImiTrade.common.exception.StockNotFoundException;
import ImiTrade.common.exception.UsernameAlreadyExistsException;
import ImiTrade.common.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central REST error translation. Every exception that escapes a controller is
 * converted into a consistent {@link ApiResponse} envelope with a stable error
 * code, so clients can rely on {@code code} for branching.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex,
                                                        HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                "Validation failed", request, details);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse> handleEmailExists(EmailAlreadyExistsException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCodes.EMAIL_ALREADY_EXISTS,
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse> handleUsernameExists(UsernameAlreadyExistsException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCodes.USERNAME_ALREADY_EXISTS,
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse> handleInvalidCredentials(InvalidCredentialsException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS,
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse> handleUserNotFound(UserNotFoundException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ApiResponse> handleStockNotFound(StockNotFoundException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCodes.STOCK_NOT_FOUND,
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidQuantityException.class)
    public ResponseEntity<ApiResponse> handleInvalidQuantity(InvalidQuantityException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_QUANTITY,
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse> handleInsufficientBalance(InsufficientBalanceException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.INSUFFICIENT_BALANCE,
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(InsufficientStockQuantityException.class)
    public ResponseEntity<ApiResponse> handleInsufficientStockQuantity(InsufficientStockQuantityException ex,
                                                                      HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.INSUFFICIENT_STOCK_QUANTITY,
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(PortfolioPositionNotFoundException.class)
    public ResponseEntity<ApiResponse> handlePortfolioPositionNotFound(PortfolioPositionNotFoundException ex,
                                                                       HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCodes.PORTFOLIO_POSITION_NOT_FOUND,
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(MarketDataUnavailableException.class)
    public ResponseEntity<ApiResponse> handleMarketDataUnavailable(MarketDataUnavailableException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.MARKET_DATA_UNAVAILABLE,
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidTickerException.class)
    public ResponseEntity<ApiResponse> handleInvalidTicker(InvalidTickerException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCodes.INVALID_TICKER,
                ex.getMessage(), request, null);
    }

    /** Thrown by Spring Security's exception translation layer. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse> handleAuthentication(AuthenticationException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED,
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ErrorCodes.ACCESS_DENIED,
                "Access denied", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpected(Exception ex,
                                                        HttpServletRequest request) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                "Internal server error", request, null);
    }

    private ResponseEntity<ApiResponse> build(HttpStatus status, String code, String message,
                                              HttpServletRequest request,
                                              Map<String, String> details) {
        ApiResponse body = new ApiResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
