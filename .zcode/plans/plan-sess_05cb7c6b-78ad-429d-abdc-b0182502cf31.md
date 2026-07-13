## Причина бага

На оси Y графика акции (`/stocks/:ticker`) отображается 4 числа вместо 3. Лишнее число — это **родная подпись последнего значения серии** (`lastValueVisible`), которая в `lightweight-charts` включена по умолчанию (`true`) и **отдельна** от опции `priceLineVisible`.

В `chartTheme.ts` отключена только `priceLineVisible: false` (строка 126) — она прячет горизонтальную пунктирную линию через холст, **но не подпись на оси Y**. Подпись на оси управляется опцией `lastValueVisible`, которую забыли выключить. Из-за этого родная подпись серии дублирует кастомную price-line текущей цены (создаваемую в `updatePriceLines`, StockPriceChart.tsx:344) — итого 4 числа.

Авто-метки ценовой шкалы (включая edge-ticks) корректно подавляются форматтером `tickmarksPriceFormatter` → `''`, рендерер пропускает пустой текст, поэтому они НЕ являются источником лишнего числа.

## План исправления (1 файл)

**`frontend/src/features/stock-details/ui/chart/chartTheme.ts`**

1. В объект `areaSeriesOptions` (строки 118-127) добавить `lastValueVisible: false` — отключает родную подпись последней цены на оси Y. После этого остаются ровно 3 подписи: min, max, current (все три — кастомные price lines из `updatePriceLines`).

2. Обновить комментарий над `areaSeriesOptions` (строки 108-117), чтобы точно отразить, что `priceLineVisible` прячет только линию, а `lastValueVisible` — подпись на оси; обе нужны в `false`, т.к. текущую цену рисуем кастомной price line единым путём с min/max.

## Что НЕ меняется
- `updatePriceLines` (StockPriceChart.tsx) — логика 3 price lines (min/max/current) уже корректна, правки не требует.
- `tickmarksPriceFormatter`, `ensureEdgeTickMarksVisible`, `tickMarkDensity` — работают как задумано, авто-метки подавлены.
- Логика коллизий `MIN_LABEL_GAP_PX` — корректна.

## Проверка
- Открыть `http://localhost:5173/stocks/CBOM` (и др. тикеры) — на оси Y должно быть ровно 3 числа: текущая цена, min, max видимого диапазона.
- Переключить периоды 1D/1W/1M/1Y, поскроллить/подрагать — лишнее число не должно появляться ни в одном сценарии.
- `npm run lint` и `npm run build` (tsc) должны проходить без ошибок.