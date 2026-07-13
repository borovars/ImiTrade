package ImiTrade.security;

import ImiTrade.common.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String ISSUER = "imitrade-test";
    private static final long TTL_MS = 3_600_000L; // 1h
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "0123456789012345678901234567890123456789012345678901234567890123".getBytes());

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, TTL_MS, ISSUER));
    }

    @DisplayName("issueToken / parseAndVerify round-trip")
    @Test
    void shouldRoundTripValidToken() {
        String token = jwtService.issueToken(42L, "alice@example.com");

        JwtService.JwtClaims claims = jwtService.parseAndVerify(token);

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("token expiry is configurable (ttl is reflected in expires_in)")
    void accessTokenTtlIsExposed() {
        assertThat(jwtService.getAccessTokenTtlMillis()).isEqualTo(TTL_MS);
    }

    @Nested
    @DisplayName("rejects invalid tokens")
    class InvalidTokens {

        @Test
        @DisplayName("null / blank token")
        void nullOrBlank() {
            assertThatThrownBy(() -> jwtService.parseAndVerify(null))
                    .isInstanceOf(AuthException.class);
            assertThatThrownBy(() -> jwtService.parseAndVerify("   "))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("tampered signature")
        void tamperedSignature() {
            String token = jwtService.issueToken(1L, "a@b.com");
            String tampered = token.substring(0, token.length() - 4) + "AAAA";
            assertThatThrownBy(() -> jwtService.parseAndVerify(tampered))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("token signed with a different key")
        void differentKey() {
            String foreignSecret = Base64.getEncoder().encodeToString(
                    "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".getBytes());
            JwtService foreign = new JwtService(new JwtProperties(foreignSecret, TTL_MS, ISSUER));
            String token = foreign.issueToken(1L, "a@b.com");

            assertThatThrownBy(() -> jwtService.parseAndVerify(token))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("token with wrong issuer")
        void wrongIssuer() {
            String token = jwtService.issueToken(1L, "a@b.com");
            JwtService otherIssuer = new JwtService(new JwtProperties(SECRET, TTL_MS, "someone-else"));
            assertThatThrownBy(() -> otherIssuer.parseAndVerify(token))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("expired token")
        void expired() throws Exception {
            JwtService shortLived = new JwtService(new JwtProperties(SECRET, 1L, ISSUER));
            String token = shortLived.issueToken(1L, "a@b.com");
            // wait past the 1ms expiry
            Thread.sleep(50L);

            assertThatThrownBy(() -> jwtService.parseAndVerify(token))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("garbage string")
        void garbage() {
            assertThatThrownBy(() -> jwtService.parseAndVerify("not-a-jwt"))
                    .isInstanceOf(AuthException.class);
        }
    }
}
