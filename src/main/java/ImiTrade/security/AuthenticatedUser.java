package ImiTrade.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated principal produced by {@link JwtAuthenticationFilter}.
 *
 * <p>Wraps the {@code userId} and {@code email} claims extracted from a verified
 * JWT and exposes them to downstream layers via {@code @AuthenticationPrincipal}.
 */
public record AuthenticatedUser(long userId, String email) implements UserDetails {

    private static final List<GrantedAuthority> AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_USER"));

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    // --- account flags: the JWT was already verified, so the account is active ---
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
