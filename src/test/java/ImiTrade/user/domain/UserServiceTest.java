package ImiTrade.user.domain;

import ImiTrade.common.exception.EmailAlreadyExistsException;
import ImiTrade.common.exception.UserNotFoundException;
import ImiTrade.common.exception.UsernameAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @DisplayName("register: hashes password, sets initial balance 500000, persists")
    @Test
    void registerSuccess() {
        String email = "alice@example.com";
        String username = "alice";
        String raw = "S3cret!pass";
        String hash = "$2a$10$hashedvalue";

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(raw)).thenReturn(hash);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User saved = userService.register(email, username, raw);

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getUsername()).isEqualTo(username);
        assertThat(saved.getPasswordHash()).isEqualTo(hash);
        // invariant: business rule "initial balance = 500000.00" must not be weakened
        assertThat(saved.getBalance()).isEqualByComparingTo(new BigDecimal("500000.0000"));
        assertThat(saved.getCreatedAt()).isNotNull();

        verify(passwordEncoder, times(1)).encode(raw);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @DisplayName("register: throws EmailAlreadyExistsException and does not persist")
    @Test
    void registerDuplicateEmail() {
        String email = "alice@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> userService.register(email, "alice", "S3cret!pass"))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @DisplayName("register: throws UsernameAlreadyExistsException and does not persist")
    @Test
    void registerDuplicateUsername() {
        String username = "alice";
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(username)).thenReturn(true);

        assertThatThrownBy(() -> userService.register("a@b.com", username, "S3cret!pass"))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @DisplayName("register: never stores a raw password")
    @Test
    void registerNeverStoresRawPassword() {
        String raw = "S3cret!pass";
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(raw)).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User saved = userService.register("a@b.com", "alice", raw);

        assertThat(saved.getPasswordHash()).isNotEqualTo(raw);
    }

    @DisplayName("getByEmail: returns user when found")
    @Test
    void getByEmailFound() {
        User user = User.builder().id(1L).email("a@b.com").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        assertThat(userService.getByEmail("a@b.com")).isSameAs(user);
    }

    @DisplayName("getByEmail: throws UserNotFoundException when missing")
    @Test
    void getByEmailMissing() {
        when(userRepository.findByEmail(eq("missing@b.com"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmail("missing@b.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @DisplayName("getById: throws UserNotFoundException when missing")
    @Test
    void getByIdMissing() {
        when(userRepository.findById(eq(99L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @DisplayName("createGuest: sets balance 100000, isGuest true, generates token")
    @Test
    void createGuestSuccess() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User guest = userService.createGuest();

        assertThat(guest.getId()).isEqualTo(1L);
        assertThat(guest.getBalance()).isEqualByComparingTo(new BigDecimal("100000.0000"));
        assertThat(guest.getIsGuest()).isTrue();
        assertThat(guest.getGuestToken()).isNotNull();
        assertThat(guest.getEmail()).isNull();
        assertThat(guest.getUsername()).isNull();
        assertThat(guest.getPasswordHash()).isNull();
    }

    @DisplayName("getByGuestToken: returns user when found")
    @Test
    void getByGuestTokenFound() {
        java.util.UUID token = java.util.UUID.randomUUID();
        User guest = User.builder().id(1L).guestToken(token).isGuest(true).build();
        when(userRepository.findByGuestToken(token)).thenReturn(Optional.of(guest));

        assertThat(userService.getByGuestToken(token)).isSameAs(guest);
    }

    @DisplayName("getByGuestToken: throws UserNotFoundException when missing")
    @Test
    void getByGuestTokenMissing() {
        java.util.UUID token = java.util.UUID.randomUUID();
        when(userRepository.findByGuestToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByGuestToken(token))
                .isInstanceOf(UserNotFoundException.class);
    }

    @DisplayName("convertGuestToRegistered: updates fields, adds bonus, clears token")
    @Test
    void convertGuestToRegisteredSuccess() {
        java.util.UUID token = java.util.UUID.randomUUID();
        User guest = User.builder()
                .id(1L)
                .balance(new BigDecimal("100000.0000"))
                .isGuest(true)
                .guestToken(token)
                .build();
        String hash = "$2a$10$hash";

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("S3cret!pass")).thenReturn(hash);

        User registered = userService.convertGuestToRegistered(guest, "alice@example.com", "alice", "S3cret!pass");

        assertThat(registered.getEmail()).isEqualTo("alice@example.com");
        assertThat(registered.getUsername()).isEqualTo("alice");
        assertThat(registered.getPasswordHash()).isEqualTo(hash);
        assertThat(registered.getIsGuest()).isFalse();
        assertThat(registered.getGuestToken()).isNull();
        assertThat(registered.getBalance()).isEqualByComparingTo(new BigDecimal("500000.0000"));
    }

    @DisplayName("convertGuestToRegistered: throws GuestAlreadyRegisteredException if already registered")
    @Test
    void convertGuestAlreadyRegistered() {
        User user = User.builder().id(1L).isGuest(false).guestToken(null).build();

        assertThatThrownBy(() -> userService.convertGuestToRegistered(user, "a@b.com", "alice", "pass"))
                .isInstanceOf(ImiTrade.common.exception.GuestAlreadyRegisteredException.class);
    }
}
