package ImiTrade.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT configuration bound from {@code app.security.jwt.*} in application.yaml.
 *
 * <pre>
 * app:
 *   security:
 *     jwt:
 *       secret-key: <base64-encoded HMAC-SHA key>
 *       access-token-ttl: 86400000   # ms (24h)
 *       issuer: imitrade
 * </pre>
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank String secretKey,
        @Min(1) long accessTokenTtl,
        @NotBlank String issuer
) {
}
