-- ============================================================
-- ImiTrade — add lot_size to stocks and backfill MOEX values.
-- Mirrors the V3 pattern: the column is added nullable first,
-- populated for every seeded row from real MOEX ISS `securities`
-- LOTSIZE values (main TQBR board), then constrained NOT NULL.
--
-- Per the lots-feature contract, lot_size is a per-stock integer
-- describing how many shares make up one tradeable lot. It is kept
-- in sync with MOEX at runtime by the price-refresh scheduler; the
-- values below are initial seeds for the ~50 catalog tickers from
-- V2 + V5. Tickers not currently trading on a MOEX main board
-- (RTPR, TCSG, LSPG, HHRU, POLY, FIVE) are seeded with their
-- historically-known lot sizes and will keep that DB value when
-- MOEX has no fresh lot size for them.
-- ============================================================

ALTER TABLE stocks ADD COLUMN lot_size INTEGER;

-- ---- V2 initial 6 stocks ---------------------------------------------------
UPDATE stocks SET lot_size = 1  WHERE ticker = 'SBER';
UPDATE stocks SET lot_size = 10 WHERE ticker = 'GAZP';
UPDATE stocks SET lot_size = 1  WHERE ticker = 'LKOH';
UPDATE stocks SET lot_size = 1  WHERE ticker = 'ROSN';
UPDATE stocks SET lot_size = 1  WHERE ticker = 'NVTK';
UPDATE stocks SET lot_size = 1  WHERE ticker = 'YDEX';

-- ---- V5 blue chips ---------------------------------------------------------
UPDATE stocks SET lot_size = 10   WHERE ticker = 'GMKN';
UPDATE stocks SET lot_size = 1    WHERE ticker = 'MGNT';
UPDATE stocks SET lot_size = 1    WHERE ticker = 'TATN';
UPDATE stocks SET lot_size = 1    WHERE ticker = 'VTBR';
UPDATE stocks SET lot_size = 10   WHERE ticker = 'MOEX';
UPDATE stocks SET lot_size = 100  WHERE ticker = 'AFKS';
UPDATE stocks SET lot_size = 1    WHERE ticker = 'PLZL';
UPDATE stocks SET lot_size = 1    WHERE ticker = 'CHMF';
UPDATE stocks SET lot_size = 10   WHERE ticker = 'NLMK';
UPDATE stocks SET lot_size = 10   WHERE ticker = 'MTSS';
UPDATE stocks SET lot_size = 10   WHERE ticker = 'RTKM';
UPDATE stocks SET lot_size = 10   WHERE ticker = 'RTPR';

-- ---- V5 second-tier / sector issuers ---------------------------------------
UPDATE stocks SET lot_size = 10    WHERE ticker = 'MAGN';
UPDATE stocks SET lot_size = 10000 WHERE ticker = 'FEES';
UPDATE stocks SET lot_size = 1000  WHERE ticker = 'HYDR';
UPDATE stocks SET lot_size = 100   WHERE ticker = 'IRAO';
UPDATE stocks SET lot_size = 100   WHERE ticker = 'SNGS';
UPDATE stocks SET lot_size = 10    WHERE ticker = 'SNGSP';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'TCSG';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'PHOR';
UPDATE stocks SET lot_size = 10    WHERE ticker = 'AFLT';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'TRNFP';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'BANE';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'LSPG';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'HHRU';
UPDATE stocks SET lot_size = 10    WHERE ticker = 'RUAL';
UPDATE stocks SET lot_size = 10    WHERE ticker = 'BSPB';
UPDATE stocks SET lot_size = 10    WHERE ticker = 'PMSB';
UPDATE stocks SET lot_size = 10    WHERE ticker = 'ALRS';
UPDATE stocks SET lot_size = 1000  WHERE ticker = 'MSNG';
UPDATE stocks SET lot_size = 100   WHERE ticker = 'CBOM';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'MTLR';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'OZON';
UPDATE stocks SET lot_size = 1000  WHERE ticker = 'UPRO';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'ENPG';
UPDATE stocks SET lot_size = 10    WHERE ticker = 'NKNC';
UPDATE stocks SET lot_size = 10    WHERE ticker = 'KMAZ';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'POLY';
UPDATE stocks SET lot_size = 1     WHERE ticker = 'FIVE';

-- ---- V5 ETF / index funds --------------------------------------------------
UPDATE stocks SET lot_size = 1 WHERE ticker = 'SBMX';
UPDATE stocks SET lot_size = 1 WHERE ticker = 'LQDT';
UPDATE stocks SET lot_size = 1 WHERE ticker = 'TMOS';
UPDATE stocks SET lot_size = 1 WHERE ticker = 'TRUR';
UPDATE stocks SET lot_size = 1 WHERE ticker = 'AKME';

ALTER TABLE stocks ALTER COLUMN lot_size SET NOT NULL;
