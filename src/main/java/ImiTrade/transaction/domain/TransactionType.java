package ImiTrade.transaction.domain;

/**
 * Direction of a trade. Mirrors the PostgreSQL {@code transaction_type} enum
 * ({@code 'BUY' | 'SELL'}) defined in {@code V1__init_schema.sql}; persisted as
 * the enum name via {@link jakarta.persistence.EnumType#STRING}.
 */
public enum TransactionType {
    BUY, SELL
}
