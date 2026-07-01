package ImiTrade.security;

import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Extracts the {@code X-Guest-Token} header, looks up the guest user via
 * {@link UserRepository} and populates the {@link SecurityContextHolder} with an
 * {@link AuthenticatedUser} principal when the token is valid and the user is still a guest.
 *
 * <p>This filter runs after {@link JwtAuthenticationFilter}. If a JWT was already
 * accepted, the context already contains an authentication and this filter does nothing.
 * If the guest token is missing, malformed, or the user is no longer a guest, the context
 * is left empty so the security chain returns 401 through the configured entry point.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuestAuthenticationFilter extends OncePerRequestFilter {

    public static final String GUEST_TOKEN_HEADER = "X-Guest-Token";

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(GUEST_TOKEN_HEADER);
        if (header == null || header.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID token;
        try {
            token = UUID.fromString(header.trim());
        } catch (IllegalArgumentException ex) {
            log.debug("Invalid guest token format: {}", header);
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findByGuestToken(token).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsGuest())) {
            log.debug("Guest token not found or user no longer a guest: {}", token);
            filterChain.doFilter(request, response);
            return;
        }

        var principal = new AuthenticatedUser(user.getId(), user.getEmail());
        var auth = new JwtAuthentication(principal);
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("Authenticated guest id={} via token={}", user.getId(), token);

        filterChain.doFilter(request, response);
    }
}
