package ImiTrade.user.domain;

import ImiTrade.common.exception.EmailAlreadyExistsException;
import ImiTrade.common.exception.GuestAlreadyRegisteredException;
import ImiTrade.common.exception.UsernameAlreadyExistsException;
import ImiTrade.common.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Application service for the {@link User} aggregate.
 *
 * <p>Encapsulates registration rules (unique email/username, BCrypt hashing,
 * the fixed virtual-money starting balance of {@link #INITIAL_BALANCE}),
 * guest creation and read access by email/id/guestToken.
 * Persistence is delegated to {@link UserRepository}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** Virtual money credited to a newly created guest. */
    public static final BigDecimal GUEST_INITIAL_BALANCE = new BigDecimal("5000.0000");

    /** Registration bonus for a guest who converts to a registered user. */
    public static final BigDecimal GUEST_REGISTRATION_BONUS = new BigDecimal("20000.0000");

    /**
     * Virtual money credited on direct registration (no guest token). Derived as
     * {@code GUEST_INITIAL_BALANCE + GUEST_REGISTRATION_BONUS} so a user who registers
     * directly ends up with the same balance as one who converts from a guest.
     */
    public static final BigDecimal INITIAL_BALANCE = GUEST_INITIAL_BALANCE.add(GUEST_REGISTRATION_BONUS);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a brand-new user.
     *
     * @param email          unique e-mail
     * @param username       unique username
     * @param rawPassword    plain-text password (will be BCrypt-hashed)
     * @return the persisted {@link User}
     * @throws EmailAlreadyExistsException    if the email is already taken
     * @throws UsernameAlreadyExistsException if the username is already taken
     */
    @Transactional
    public User register(String email, String username, String rawPassword) {
        log.debug("Registering user: email='{}', username='{}'", email, username);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        User user = User.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .balance(INITIAL_BALANCE)
                .isGuest(false)
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Registered user id={} email='{}'", saved.getId(), saved.getEmail());
        return saved;
    }

    /**
     * Creates a new guest user with a random token and the guest starting balance.
     *
     * @return the persisted guest {@link User}
     */
    @Transactional
    public User createGuest() {
        UUID token = UUID.randomUUID();
        User guest = User.builder()
                .balance(GUEST_INITIAL_BALANCE)
                .isGuest(true)
                .guestToken(token)
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(guest);
        log.info("Created guest id={} token={}", saved.getId(), saved.getGuestToken());
        return saved;
    }

    /**
     * Converts an existing guest into a registered user, preserving balance,
     * portfolio and transactions. Adds the registration bonus.
     *
     * @param guest       the guest user to convert
     * @param email       unique e-mail
     * @param username    unique username
     * @param rawPassword plain-text password (will be BCrypt-hashed)
     * @return the updated {@link User} now registered
     * @throws GuestAlreadyRegisteredException if the user is already registered
     * @throws EmailAlreadyExistsException     if the email is already taken
     * @throws UsernameAlreadyExistsException  if the username is already taken
     */
    @Transactional
    public User convertGuestToRegistered(User guest, String email, String username, String rawPassword) {
        if (!Boolean.TRUE.equals(guest.getIsGuest())) {
            throw new GuestAlreadyRegisteredException(guest.getGuestToken() != null ? guest.getGuestToken().toString() : "null");
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        guest.setEmail(email);
        guest.setUsername(username);
        guest.setPasswordHash(passwordEncoder.encode(rawPassword));
        guest.setIsGuest(false);
        guest.setGuestToken(null);
        guest.setBalance(guest.getBalance().add(GUEST_REGISTRATION_BONUS));

        log.info("Converted guest id={} to registered user email='{}'", guest.getId(), email);
        return guest;
    }

    /**
     * Looks up a guest user by its token.
     *
     * @throws UserNotFoundException if no guest with the given token exists
     */
    @Transactional(readOnly = true)
    public User getByGuestToken(UUID token) {
        return userRepository.findByGuestToken(token)
                .orElseThrow(() -> new UserNotFoundException("guestToken=" + token));
    }

    /**
     * Looks up a user by e-mail (used by login).
     *
     * @throws UserNotFoundException if no user with the given email exists
     */
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email=" + email));
    }

    /**
     * Looks up a user by id (used by JWT-authenticated requests).
     *
     * @throws UserNotFoundException if no user with the given id exists
     */
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("id=" + id));
    }
}
