package ImiTrade.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest", description = "Payload for POST /api/v1/auth/register")
public record RegisterRequest(

        @Schema(description = "Unique user e-mail", example = "alice@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Unique username", example = "alice")
        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                message = "username may only contain letters, digits, '_', '-' and '.'")
        String username,

        @Schema(description = "Plain-text password (will be BCrypt-hashed)", example = "S3cret!pass")
        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}
