/**
 * DTO аутентификации по контракту backend
 * (`src/main/java/ImiTrade/auth/dto/`).
 *
 * Имена полей ответа — snake_case, как их сериализует Jackson
 * (`@JsonProperty("expires_in")` в `AuthResponse`).
 * `BigDecimal` → number, `Instant` → ISO-строка (Jackson по умолчанию).
 */

/** `POST /api/v1/auth/login` body. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** `POST /api/v1/auth/register` body. `guestToken` — для конвертации гостя. */
export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  guestToken?: string;
}

/** Ответ `/auth/login` и `/auth/register`: выпущенный JWT. */
export interface AuthResponse {
  token: string;
  type: string;
  expires_in: number;
}

/** `GET /api/v1/users/me` — профиль аутентифицированного пользователя. */
export interface CurrentUserResponse {
  id: number;
  email: string;
  username: string;
  balance: number;
  createdAt: string;
}
