-- ============================================================
-- ImiTrade — seed initial stocks catalog
-- Schema is unchanged (no new tables); only seed data is inserted.
-- ============================================================

INSERT INTO stocks (ticker, company_name, exchange) VALUES
    ('AAPL',  'Apple Inc.',            'NASDAQ'),
    ('MSFT',  'Microsoft Corporation', 'NASDAQ'),
    ('NVDA',  'NVIDIA Corporation',    'NASDAQ'),
    ('AMZN',  'Amazon.com Inc.',       'NASDAQ'),
    ('TSLA',  'Tesla Inc.',            'NASDAQ'),
    ('GOOGL', 'Alphabet Inc.',         'NASDAQ');
