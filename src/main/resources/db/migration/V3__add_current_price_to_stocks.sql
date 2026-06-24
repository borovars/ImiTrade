-- ============================================================
-- ImiTrade — add current_price to stocks and seed test prices.
-- Schema is unchanged otherwise (no new tables). The column is
-- added nullable first, populated for the existing seed rows, then
-- constrained NOT NULL so existing data always has a value.
-- ============================================================

ALTER TABLE stocks ADD COLUMN current_price NUMERIC(19,4);

UPDATE stocks SET current_price = 212.3500 WHERE ticker = 'AAPL';
UPDATE stocks SET current_price = 415.2000 WHERE ticker = 'MSFT';
UPDATE stocks SET current_price = 120.4500 WHERE ticker = 'NVDA';
UPDATE stocks SET current_price = 185.7000 WHERE ticker = 'AMZN';
UPDATE stocks SET current_price = 175.3000 WHERE ticker = 'TSLA';
UPDATE stocks SET current_price = 178.9000 WHERE ticker = 'GOOGL';

ALTER TABLE stocks ALTER COLUMN current_price SET NOT NULL;
