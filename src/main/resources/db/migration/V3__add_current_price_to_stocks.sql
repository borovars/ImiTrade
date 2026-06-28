-- ============================================================
-- ImiTrade — add current_price to stocks and seed test prices.
-- Schema is unchanged otherwise (no new tables). The column is
-- added nullable first, populated for the existing seed rows, then
-- constrained NOT NULL so existing data always has a value.
-- ============================================================

ALTER TABLE stocks ADD COLUMN current_price NUMERIC(19,4);

UPDATE stocks SET current_price = 310.5000 WHERE ticker = 'SBER';
UPDATE stocks SET current_price = 170.2000 WHERE ticker = 'GAZP';
UPDATE stocks SET current_price = 6800.0000 WHERE ticker = 'LKOH';
UPDATE stocks SET current_price = 620.0000 WHERE ticker = 'ROSN';
UPDATE stocks SET current_price = 1850.0000 WHERE ticker = 'NVTK';
UPDATE stocks SET current_price = 4100.0000 WHERE ticker = 'YDEX';

ALTER TABLE stocks ALTER COLUMN current_price SET NOT NULL;
