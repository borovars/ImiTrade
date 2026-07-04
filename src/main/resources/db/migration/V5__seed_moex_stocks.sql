-- ============================================================
-- ImiTrade — extend MOEX-oriented stocks catalog
-- Schema is unchanged (no new tables/columns); only seed data is
-- inserted. Adds 44 MOEX-compatible instruments on top of the 6
-- rows seeded by V2/V3, bringing the catalog to ~50 tickers:
-- liquid Russian blue chips, second-tier issuers and a few ETFs.
-- Prices are plausible MOEX-style starting values; the MOEX price
-- scheduler refreshes them at runtime. No ticker duplicates the
-- existing seed (SBER/GAZP/LKOH/ROSN/NVTK/YDEX).
-- ============================================================

INSERT INTO stocks (ticker, company_name, exchange, current_price) VALUES
    -- ---- Liquid Russian blue chips -----------------------------------------
    ('GMKN',  'ПАО «ГМК «Норильский никель»',     'MOEX', 13400.0000),
    ('MGNT',  'ПАО «Магнит»',                     'MOEX',  4800.0000),
    ('TATN',  'ПАО «Татнефть»',                   'MOEX',   720.0000),
    ('VTBR',  'Банк ВТБ',                         'MOEX',    13.0000),
    ('MOEX',  'ПАО «Московская Биржа ММВБ-РТС»',  'MOEX',   235.0000),
    ('AFKS',  'АФК «Система»',                    'MOEX',    22.0000),
    ('PLZL',  'ПАО «Полюс»',                      'MOEX', 13200.0000),
    ('CHMF',  'ПАО «Северсталь»',                 'MOEX', 10800.0000),
    ('NLMK',  'ПАО «НЛМК»',                       'MOEX',   165.0000),
    ('MTSS',  'ПАО «МТС»',                        'MOEX',   245.0000),
    ('RTKM',  'ПАО «Ростелеком»',                 'MOEX',    95.0000),
    ('RTPR',  'ПАО «Ростелеком» (прив.)',         'MOEX',   115.0000),
    -- ---- Second-tier / sector issuers --------------------------------------
    ('MAGN',  'ПАО «ММК»',                        'MOEX',    52.0000),
    ('FEES',  'ПАО «ФСК ЕЭС»',                    'MOEX',     0.2000),
    ('HYDR',  'ПАО «РусГидро»',                   'MOEX',     1.2000),
    ('IRAO',  'ПАО «Интер РАО»',                  'MOEX',    15.5000),
    ('SNGS',  'ПАО «Сургутнефтегаз»',             'MOEX',    28.0000),
    ('SNGSP', 'ПАО «Сургутнефтегаз» (прив.)',     'MOEX',    52.0000),
    ('TCSG',  'АО «Т-Технологии»',                'MOEX',  6800.0000),
    ('PHOR',  'ПАО «ФосАгро»',                    'MOEX',  6900.0000),
    ('AFLT',  'ПАО «Аэрофлот»',                   'MOEX',    55.0000),
    ('TRNFP', 'ПАО «Транснефть» (прив.)',         'MOEX',  1450.0000),
    ('BANE',  'ПАО АНК «Башнефть»',               'MOEX',  1650.0000),
    ('LSPG',  'ПАО «Лента»',                      'MOEX',   950.0000),
    ('HHRU',  'HeadHunter Group PLC',             'MOEX',  2800.0000),
    ('RUAL',  'ОК РУСАЛ',                         'MOEX',    32.0000),
    ('BSPB',  'ПАО «Банк «Санкт-Петербург»',      'MOEX',   290.0000),
    ('PMSB',  'ПАО «МТС-Банк»',                   'MOEX',   165.0000),
    ('ALRS',  'АК «АЛРОСА» (ПАО)',                'MOEX',    65.0000),
    ('MSNG',  'ПАО «Мосэнерго»',                  'MOEX',     2.5000),
    ('CBOM',  'ПАО «МКБ»',                        'MOEX',    13.0000),
    ('MTLR',  'ПАО «Мечел»',                      'MOEX',   195.0000),
    ('OZON',  'Ozon Holdings PLC',                'MOEX',  2500.0000),
    ('UPRO',  'ПАО «Юнипро»',                     'MOEX',     2.7000),
    ('ENPG',  'En+ Group PLC',                    'MOEX',   410.0000),
    ('NKNC',  'ПАО «Казаньоргсинтез»',            'MOEX',   245.0000),
    ('KMAZ',  'ПАО «КАМАЗ»',                      'MOEX',    95.0000),
    ('POLY',  'Polymetal International plc',      'MOEX',   900.0000),
    ('FIVE',  'X5 Retail Group N.V.',             'MOEX',  2100.0000),
    -- ---- ETF / index funds --------------------------------------------------
    ('SBMX',  'ВТБ — Индекс Мосбиржи (ETF)',      'MOEX',  8500.0000),
    ('LQDT',  'Фонд Ликвидность (ETF)',           'MOEX',  9200.0000),
    ('TMOS',  'Т-Индекс Мосбиржи (ETF)',          'MOEX',  4100.0000),
    ('TRUR',  'FinEx Безрисковый рублевый (ETF)', 'MOEX',  1050.0000),
    ('AKME',  'Альфа-Менделеев (ETF)',            'MOEX',  1200.0000);
