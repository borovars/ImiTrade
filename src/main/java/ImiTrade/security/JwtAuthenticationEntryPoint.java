package ImiTrade.security;

import ImiTrade.common.web.ApiResponse;
import ImiTrade.common.web.ErrorCodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Returns a 401 JSON body for any request that reaches a secured endpoint
 * without a valid JWT (or with an invalid one). Registered as the chain's
 * {@code authenticationEntryPoint} in {@link SecurityFilterChainConfig}.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ApiResponse body = new ApiResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                ErrorCodes.UNAUTHENTICATED,
                "Authentication required: a valid Bearer JWT must be supplied",
                request.getRequestURI(),
                null
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
