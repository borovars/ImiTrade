package ImiTrade.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Payload for POST /api/v1/auth/login")
public record LoginRequest(

        @Schema(description = "User e-mail", example = "alice@example.com")
        @NotBlank
        @Email
        String email,

        @Schema(description = "Plain-text password", example = "S3cret!pass")
        @NotBlank
        String password
) {
}
