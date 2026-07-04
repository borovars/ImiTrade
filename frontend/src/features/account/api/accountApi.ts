import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { AccountResponse } from '../types/accountTypes';

export async function getAccount(): Promise<AccountResponse> {
  const response = await apiClient.get<AccountResponse>(API_ENDPOINTS.ACCOUNT.BASE);
  return response.data;
}
