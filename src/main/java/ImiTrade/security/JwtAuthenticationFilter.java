package ImiTrade.security;

import ImiTrade.common.exception.AuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extracts the {@code Authorization: Bearer <jwt>} header, verifies the token
 * via {@link JwtService} and populates the {@link SecurityContextHolder} with an
 * {@link AuthenticatedUser} principal.
 *
 * <p>This filter does NOT throw on a missing/bad token by itself — instead it
 * leaves the context empty so that {@link SecurityFilterChainConfig} denies the
 * request through the configured {@link AuthenticationEntryPoint}. When a token
 * <em>is</em> present but invalid, the failure is delegated to the entry point
 * so the client gets a consistent 401 JSON response.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            JwtService.JwtClaims claims = jwtService.parseAndVerify(token);
            var principal = new AuthenticatedUser(claims.userId(), claims.email());
            var auth = new JwtAuthentication(principal);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated user id={} via JWT", claims.userId());
        } catch (AuthException ex) {
            SecurityContextHolder.clearContext();
            AuthenticationException securityEx = new BadCredentialsException(ex.getMessage(), ex);
            authenticationEntryPoint.commence(request, response, securityEx);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
