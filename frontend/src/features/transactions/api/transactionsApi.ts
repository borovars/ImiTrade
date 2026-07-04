import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { TransactionPage } from '../types/transactionsTypes';

/** История торговых операций пользователя (Spring Page, DESC по createdAt). */
export async function getTransactions(): Promise<TransactionPage> {
  const response = await apiClient.get<TransactionPage>(API_ENDPOINTS.TRANSACTIONS.BASE);
  // Backend отдаёт Spring Page, разворачиваем axios-ответ.
  return response.data;
}
