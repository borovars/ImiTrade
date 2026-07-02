export interface ApiErrorResponse {
  message: string;
  status: number;
}

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
    this.name = 'ApiError';
  }
}

export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }

  if (typeof error === 'object' && error !== null) {
    const err = error as Record<string, unknown>;

    if (err.response && typeof err.response === 'object') {
      const response = err.response as Record<string, unknown>;
      const data = response.data as Record<string, unknown> | undefined;
      const status = (response.status as number) || 500;

      const message =
        (data?.message as string) ||
        (err.message as string) ||
        'Произошла неизвестная ошибка';

      return new ApiError(message, status);
    }

    if (typeof err.message === 'string') {
      return new ApiError(err.message, (err.status as number) || 500);
    }
  }

  return new ApiError('Произошла неизвестная ошибка', 500);
}
