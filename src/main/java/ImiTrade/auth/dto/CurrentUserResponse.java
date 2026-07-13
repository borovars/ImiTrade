package ImiTrade.auth.dto;

import ImiTrade.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "CurrentUserResponse", description = "Profile of the authenticated user")
public record CurrentUserResponse(
        Long id,
        String email,
        String username,
        BigDecimal balance,
        Instant createdAt
) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getBalance(),
                user.getCreatedAt()
        );
    }
}
