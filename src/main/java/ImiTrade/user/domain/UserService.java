package ImiTrade.user.domain;

import ImiTrade.common.exception.EmailAlreadyExistsException;
import ImiTrade.common.exception.UsernameAlreadyExistsException;
import ImiTrade.common.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Application service for the {@link User} aggregate.
 *
 * <p>Encapsulates registration rules (unique email/username, BCrypt hashing,
 * the fixed virtual-money starting balance of {@link #INITIAL_BALANCE}) and
 * read access by email/id. Persistence is delegated to {@link UserRepository}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** Virtual money credited to every newly registered user (must not be changed). */
    public static final BigDecimal INITIAL_BALANCE = new BigDecimal("500000.0000");

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
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Registered user id={} email='{}'", saved.getId(), saved.getEmail());
        return saved;
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
