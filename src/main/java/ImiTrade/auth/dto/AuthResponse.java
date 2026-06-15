package ImiTrade.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponse", description = "Token issued on successful login/registration")
public record AuthResponse(

        @Schema(description = "JWT access token", example = "eyJhbGciOi...")
        @JsonProperty("token")
        String token,

        @Schema(description = "Token type", example = "Bearer")
        @JsonProperty("type")
        String type,

        @Schema(description = "Lifetime in seconds", example = "86400")
        @JsonProperty("expires_in")
        long expiresIn
) {
}
