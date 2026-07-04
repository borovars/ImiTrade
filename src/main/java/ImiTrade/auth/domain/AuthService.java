package ImiTrade.auth.domain;

import ImiTrade.auth.dto.AuthResponse;
import ImiTrade.auth.dto.LoginRequest;
import ImiTrade.auth.dto.RegisterRequest;
import ImiTrade.common.exception.InvalidGuestTokenException;
import ImiTrade.common.exception.InvalidCredentialsException;
import ImiTrade.common.exception.UserNotFoundException;
import ImiTrade.security.JwtService;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Orchestration layer for authentication: registration + login.
 *
 * <p>Delegates persistence and uniqueness checks to {@link UserService},
 * BCrypt verification to the {@link PasswordEncoder} and token issuance to
 * {@link JwtService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user or converts an existing guest if a guest token is provided.
     *
     * @return a {@link AuthResponse} containing the access token
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user;
        if (StringUtils.hasText(request.guestToken())) {
            UUID token;
            try {
                token = UUID.fromString(request.guestToken());
            } catch (IllegalArgumentException ex) {
                throw new InvalidGuestTokenException(request.guestToken());
            }
            User guest = userService.getByGuestToken(token);
            user = userService.convertGuestToRegistered(guest, request.email(), request.username(), request.password());
        } else {
            user = userService.register(request.email(), request.username(), request.password());
        }
        return toAuthResponse(user);
    }

    /**
     * Authenticates a user by e-mail and password and issues a JWT.
     *
     * @throws InvalidCredentialsException on any mismatch (no user-enumeration leak)
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user;
        try {
            user = userService.getByEmail(request.email());
        } catch (UserNotFoundException notFound) {
            // Translate "no such user" into the same 401 as a bad password so a
            // failed login never leaks whether the e-mail is registered.
            log.debug("Login failed: unknown email='{}'", request.email());
            throw new InvalidCredentialsException();
        }

        // Constant-time comparison is provided by BCryptPasswordEncoder.matches
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.debug("Login failed: bad password for email='{}'", request.email());
            throw new InvalidCredentialsException();
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.issueToken(user.getId(), user.getEmail());
        long expiresInSeconds = jwtService.getAccessTokenTtlMillis() / 1000;
        return new AuthResponse(token, "Bearer", expiresInSeconds);
    }
}
