const STORAGE_KEYS = {
  JWT_TOKEN: 'imitrade_jwt_token',
  GUEST_TOKEN: 'imitrade_guest_token',
  ONBOARDING_COMPLETED: 'imitrade_onboarding_completed',
} as const;

export const storage = {
  getJwtToken(): string | null {
    return localStorage.getItem(STORAGE_KEYS.JWT_TOKEN);
  },
  setJwtToken(token: string): void {
    localStorage.setItem(STORAGE_KEYS.JWT_TOKEN, token);
  },
  removeJwtToken(): void {
    localStorage.removeItem(STORAGE_KEYS.JWT_TOKEN);
  },
  getGuestToken(): string | null {
    return localStorage.getItem(STORAGE_KEYS.GUEST_TOKEN);
  },
  setGuestToken(token: string): void {
    localStorage.setItem(STORAGE_KEYS.GUEST_TOKEN, token);
  },
  removeGuestToken(): void {
    localStorage.removeItem(STORAGE_KEYS.GUEST_TOKEN);
  },
  clearAuth(): void {
    this.removeJwtToken();
    this.removeGuestToken();
  },
  /**
   * Пройден ли онбординг (приветственное окно при первом визите).
   * Флаг хранится как строка 'true'; при отсутствии считается непройденным.
   */
  isOnboardingCompleted(): boolean {
    return localStorage.getItem(STORAGE_KEYS.ONBOARDING_COMPLETED) === 'true';
  },
  setOnboardingCompleted(): void {
    localStorage.setItem(STORAGE_KEYS.ONBOARDING_COMPLETED, 'true');
  },
};
