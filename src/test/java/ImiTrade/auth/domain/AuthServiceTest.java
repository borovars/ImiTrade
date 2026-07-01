package ImiTrade.auth.domain;

import ImiTrade.auth.dto.AuthResponse;
import ImiTrade.auth.dto.LoginRequest;
import ImiTrade.auth.dto.RegisterRequest;
import ImiTrade.common.exception.EmailAlreadyExistsException;
import ImiTrade.common.exception.InvalidCredentialsException;
import ImiTrade.security.JwtService;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, jwtService, passwordEncoder);
    }

    @DisplayName("register: delegates to UserService and returns a token")
    @Test
    void registerReturnsToken() {
        RegisterRequest req = new RegisterRequest("alice@example.com", "alice", "S3cret!pass", null);
        User user = sampleUser();
        when(userService.register(req.email(), req.username(), req.password())).thenReturn(user);
        when(jwtService.issueToken(user.getId(), user.getEmail())).thenReturn("jwt-token");
        when(jwtService.getAccessTokenTtlMillis()).thenReturn(3_600_000L);

        AuthResponse res = authService.register(req);

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.type()).isEqualTo("Bearer");
        assertThat(res.expiresIn()).isEqualTo(3600L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @DisplayName("register: with guestToken converts guest and returns a token")
    @Test
    void registerWithGuestTokenConvertsGuest() {
        String token = "8c33bb2e-8d4b-4e0c-b57d-0dce5f6c4e3f";
        RegisterRequest req = new RegisterRequest("alice@example.com", "alice", "S3cret!pass", token);
        User guest = sampleGuest();
        User registered = sampleUser();
        when(userService.getByGuestToken(java.util.UUID.fromString(token))).thenReturn(guest);
        when(userService.convertGuestToRegistered(guest, req.email(), req.username(), req.password())).thenReturn(registered);
        when(jwtService.issueToken(registered.getId(), registered.getEmail())).thenReturn("jwt-token");
        when(jwtService.getAccessTokenTtlMillis()).thenReturn(3_600_000L);

        AuthResponse res = authService.register(req);

        assertThat(res.token()).isEqualTo("jwt-token");
        verify(userService, never()).register(anyString(), anyString(), anyString());
    }

    @DisplayName("register: invalid guestToken throws InvalidGuestTokenException")
    @Test
    void registerWithInvalidGuestToken() {
        RegisterRequest req = new RegisterRequest("alice@example.com", "alice", "S3cret!pass", "not-a-uuid");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ImiTrade.common.exception.InvalidGuestTokenException.class);
    }

    @DisplayName("register: propagates duplicate-email error from UserService")
    @Test
    void registerPropagatesConflict() {
        RegisterRequest req = new RegisterRequest("alice@example.com", "alice", "S3cret!pass", null);
        when(userService.register(anyString(), anyString(), anyString()))
                .thenThrow(new EmailAlreadyExistsException("alice@example.com"));

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(jwtService, never()).issueToken(eq(1L), anyString());
    }

    @DisplayName("login: success returns token")
    @Test
    void loginSuccess() {
        LoginRequest req = new LoginRequest("alice@example.com", "S3cret!pass");
        User user = sampleUser();
        when(userService.getByEmail(req.email())).thenReturn(user);
        when(passwordEncoder.matches(req.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.issueToken(user.getId(), user.getEmail())).thenReturn("jwt-token");
        when(jwtService.getAccessTokenTtlMillis()).thenReturn(3_600_000L);

        AuthResponse res = authService.login(req);

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.type()).isEqualTo("Bearer");
    }

    @DisplayName("login: bad password -> InvalidCredentialsException (no user enumeration)")
    @Test
    void loginBadPassword() {
        LoginRequest req = new LoginRequest("alice@example.com", "wrong");
        User user = sampleUser();
        when(userService.getByEmail(req.email())).thenReturn(user);
        when(passwordEncoder.matches(req.password(), user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).issueToken(eq(1L), anyString());
    }

    @DisplayName("login: unknown user -> InvalidCredentialsException (no user enumeration)")
    @Test
    void loginUnknownUser() {
        LoginRequest req = new LoginRequest("ghost@example.com", "S3cret!pass");
        when(userService.getByEmail(req.email()))
                .thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).issueToken(eq(1L), anyString());
    }

    private static User sampleUser() {
        return User.builder()
                .id(1L)
                .email("alice@example.com")
                .username("alice")
                .passwordHash("$2a$10$hash")
                .balance(new BigDecimal("500000.0000"))
                .isGuest(false)
                .createdAt(Instant.now())
                .build();
    }

    private static User sampleGuest() {
        return User.builder()
                .id(2L)
                .balance(new BigDecimal("100000.0000"))
                .isGuest(true)
                .guestToken(java.util.UUID.fromString("8c33bb2e-8d4b-4e0c-b57d-0dce5f6c4e3f"))
                .createdAt(Instant.now())
                .build();
    }
}
