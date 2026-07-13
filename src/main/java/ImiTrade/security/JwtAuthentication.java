package ImiTrade.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Spring Security {@link Authentication} backed by a verified JWT.
 * Carries the {@link AuthenticatedUser} principal and no credentials.
 */
public record JwtAuthentication(AuthenticatedUser principal) implements Authentication {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return principal.getAuthorities();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        if (!isAuthenticated) {
            throw new IllegalArgumentException("Cannot mark a JWT authentication as unauthenticated");
        }
    }

    @Override
    public String getName() {
        return principal.email();
    }
}
