-- ============================================================
-- ImiTrade — seed initial stocks catalog
-- Schema is unchanged (no new tables); only seed data is inserted.
-- ============================================================

INSERT INTO stocks (ticker, company_name, exchange) VALUES
    ('SBER',  'Сбербанк',              'MOEX'),
    ('GAZP',  'Газпром',               'MOEX'),
    ('LKOH',  'ЛУКОЙЛ',               'MOEX'),
    ('ROSN',  'Роснефть',              'MOEX'),
    ('NVTK',  'НОВАТЭК',              'MOEX'),
    ('YDEX',  'Яндекс',                'MOEX');
