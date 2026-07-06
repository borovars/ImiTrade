export interface GuestResponse {
  guestToken: string;
  balance: string;
}

export interface ApiErrorPayload {
  message: string;
  status: number;
}
