import { useEffect, useState } from 'react';
import {
  Container,
  Typography,
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  InputAdornment,
} from '@mui/material';
import { Search } from 'lucide-react';
import { useStocksQuery } from '@/features/stocks/model/useStocksQuery';
import StocksTable from '@/features/stocks/ui/StocksTable';
import { StateError, StateEmpty, TableSkeleton } from '@/shared/components';

// По умолчанию 10 строк на страницу — увеличенные строки каталога дают ~10
// акций на экран без прокрутки.
const DEFAULT_ROWS_PER_PAGE = 10;

// Задержка дебаунса поиска (мс): избегаем запроса на каждый символ.
const SEARCH_DEBOUNCE_MS = 300;

/**
 * Варианты сортировки каталога. Каждый сопоставлен с одним Spring Data
 * expression, который backend принимает через `@PageableDefault Pageable`
 * (`sort=ticker,asc` и т.д.). Направление выбирается пользователем.
 */
const SORT_OPTIONS = [
  { value: 'ticker,asc', label: 'Алфавитный порядок (А→Я)' },
  { value: 'ticker,desc', label: 'Алфавитный порядок (Я→А)' },
  { value: 'currentPrice,asc', label: 'Цена (по возрастанию)' },
  { value: 'currentPrice,desc', label: 'Цена (по убыванию)' },
  { value: 'lotSize,asc', label: 'Размер лота (по возрастанию)' },
  { value: 'lotSize,desc', label: 'Размер лота (по убыванию)' },
] as const;

const DEFAULT_SORT = 'ticker,asc';

export default function StocksPage() {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(DEFAULT_ROWS_PER_PAGE);
  const [sort, setSort] = useState<string>(DEFAULT_SORT);

  // Поиск: `searchInput` — мгновенно отражает ввод; `search` — дебаунсенное
  // значение, которое уходит в запрос (меняется не чаще, чем раз в
  // SEARCH_DEBOUNCE_MS).
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    const handle = setTimeout(() => {
      setSearch(searchInput.trim());
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(handle);
  }, [searchInput]);

  const { data, isLoading, isError, error, refetch, isPlaceholderData } = useStocksQuery(
    page,
    rowsPerPage,
    sort,
    search
  );

  const handlePageChange = (newPage: number) => setPage(newPage);

  const handleRowsPerPageChange = (newSize: number) => {
    setRowsPerPage(newSize);
    setPage(0);
  };

  // Смена сортировки или поиска сбрасывает на первую страницу (иначе можно
  // попасть на несуществующую страницу нового порядка/фильтра).
  const handleSortChange = (newSort: string) => {
    setSort(newSort);
    setPage(0);
  };

  const handleSearchChange = (value: string) => {
    setSearchInput(value);
    setPage(0);
  };

  return (
    <Container maxWidth="lg">
      <Box
        sx={{
          mt: 4,
          mb: 3,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        <Typography variant="h4" component="h1">
          Акции
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
          <TextField
            size="small"
            label="Поиск"
            value={searchInput}
            onChange={(e) => handleSearchChange(e.target.value)}
            sx={{ minWidth: 260 }}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <Search size={18} />
                  </InputAdornment>
                ),
              },
            }}
          />
          <FormControl size="small" sx={{ minWidth: 260 }} disabled={isPlaceholderData}>
            <InputLabel id="stocks-sort-label">Сортировка</InputLabel>
            <Select
              labelId="stocks-sort-label"
              id="stocks-sort"
              label="Сортировка"
              value={sort}
              onChange={(e) => handleSortChange(e.target.value)}
            >
              {SORT_OPTIONS.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
      </Box>

      {isLoading && <TableSkeleton />}

      {isError && <StateError title="Не удалось загрузить акции" error={error} onRetry={refetch} />}

      {data && data.content.length === 0 && !isLoading && !isError && (
        <StateEmpty
          title="Ничего не найдено"
          helperText={
            search ? `По запросу «${search}» нет акций.` : 'Акции отсутствуют.'
          }
        />
      )}

      {data && data.content.length > 0 && (
        <StocksTable
          stocks={data.content}
          page={data.number}
          rowsPerPage={data.size}
          totalElements={data.totalElements}
          loading={isPlaceholderData}
          onPageChange={handlePageChange}
          onRowsPerPageChange={handleRowsPerPageChange}
        />
      )}
    </Container>
  );
}
