package ImiTrade.security;

import ImiTrade.common.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * Stateless JWT issuance / verification service.
 *
 * <p>Tokens are signed with HMAC-SHA256 (HS256). Claims:
 * <ul>
 *   <li>{@code sub}     — user id (as string)</li>
 *   <li>{@code email}   — user e-mail</li>
 *   <li>{@code iss}     — configured issuer</li>
 *   <li>{@code iat}/{@code exp} — issued-at / expiry</li>
 * </ul>
 */
@Slf4j
@Service
public class JwtService {

    public static final String CLAIM_EMAIL = "email";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = loadKey(properties.secretKey());
    }

    private static SecretKey loadKey(String secretKey) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secretKey);
        } catch (IllegalArgumentException notBase64) {
            // allow a plain UTF-8 key when not base64-encoded
            bytes = secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    /** Issues a signed access token for the given user id and e-mail. */
    public String issueToken(long userId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(properties.accessTokenTtl());
        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim(CLAIM_EMAIL, email)
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /** Returns the expiry timestamp used for issued tokens (ms since epoch). */
    public long getAccessTokenTtlMillis() {
        return properties.accessTokenTtl();
    }

    /**
     * Parses and verifies a token.
     *
     * @throws AuthException if the token is missing, malformed, expired, or has
     *                       an invalid signature
     */
    public JwtClaims parseAndVerify(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthException("Missing bearer token");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            long userId = Long.parseLong(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            if (email == null) {
                throw new AuthException("Token is missing 'email' claim");
            }
            return new JwtClaims(userId, email);
        } catch (JwtException e) {
            throw new AuthException("Invalid or expired token", e);
        } catch (NumberFormatException e) {
            throw new AuthException("Token subject is not a valid user id", e);
        }
    }

    /** Strongly typed payload of a verified JWT. */
    public record JwtClaims(long userId, String email) {
    }
}
