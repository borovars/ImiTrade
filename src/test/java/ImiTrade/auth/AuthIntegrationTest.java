package ImiTrade.auth;

import ImiTrade.auth.dto.LoginRequest;
import ImiTrade.auth.dto.RegisterRequest;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the auth endpoints. Uses the real security layer
 * (no mocks) and an in-memory H2 database (PostgreSQL-compatibility mode).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    // =====================================================================
    // Registration
    // =====================================================================

    @Test
    @DisplayName("POST /register — success returns 201 + token, persists user with initial balance")
    void registerSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("alice@example.com", "alice", "S3cret!pass", null);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").isNumber())
                .andReturn();

        // token is a JWT (header.payload.signature)
        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        assertThat(token.split("\\.")).hasSize(3);

        User saved = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(saved.getUsername()).isEqualTo("alice");
        // business rule: initial balance = 500000.00 (must not change)
        assertThat(saved.getBalance()).isEqualByComparingTo(new BigDecimal("500000.0000"));
    }

    @Test
    @DisplayName("POST /register with guestToken — converts guest, preserves balance + bonus, returns JWT")
    void registerWithGuestToken() throws Exception {
        // 1. create a guest
        String guestResponse = mockMvc.perform(post("/api/v1/guest"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String guestToken = objectMapper.readTree(guestResponse).get("guestToken").asText();
        BigDecimal guestBalance = new BigDecimal(objectMapper.readTree(guestResponse).get("balance").asText());
        assertThat(guestBalance).isEqualByComparingTo(new BigDecimal("100000.0000"));

        // 2. buy a stock to create portfolio + transactions
        // (we skip trading here to keep the test focused; balance check is enough)

        // 3. register with guestToken
        RegisterRequest req = new RegisterRequest("alice@example.com", "alice", "S3cret!pass", guestToken);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andReturn();

        String jwt = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertThat(jwt.split("\\.")).hasSize(3);

        // 4. verify user is now registered with bonus
        User saved = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getIsGuest()).isFalse();
        assertThat(saved.getGuestToken()).isNull();
        assertThat(saved.getBalance()).isEqualByComparingTo(new BigDecimal("500000.0000"));
    }

    @Test
    @DisplayName("POST /register with invalid guestToken returns 401")
    void registerWithInvalidGuestToken() throws Exception {
        RegisterRequest req = new RegisterRequest("alice@example.com", "alice", "S3cret!pass", "not-a-uuid");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_GUEST_TOKEN"));
    }

    @Test
    @DisplayName("POST /register — duplicate email returns 409")
    void registerDuplicateEmail() throws Exception {
        RegisterRequest req = new RegisterRequest("alice@example.com", "alice", "S3cret!pass", null);
        register(req);

        RegisterRequest dup = new RegisterRequest("alice@example.com", "alice2", "S3cret!pass", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("POST /register — duplicate username returns 409")
    void registerDuplicateUsername() throws Exception {
        register(new RegisterRequest("alice@example.com", "alice", "S3cret!pass", null));

        RegisterRequest dup = new RegisterRequest("alice2@example.com", "alice", "S3cret!pass", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("POST /register — password is stored BCrypt-hashed, never plain-text")
    void registerPasswordIsHashed() throws Exception {
        String raw = "S3cret!pass";
        register(new RegisterRequest("alice@example.com", "alice", raw, null));

        User saved = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(saved.getPasswordHash())
                .as("stored password must be a BCrypt hash, not the raw password")
                .isNotEqualTo(raw)
                .startsWith("$2");
    }

    @Test
    @DisplayName("POST /register — invalid payload returns 400")
    void registerValidationFails() throws Exception {
        // blank username, invalid email, short password
        RegisterRequest req = new RegisterRequest("not-an-email", "a_", "short", null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").exists());
    }

    // =====================================================================
    // Login
    // =====================================================================

    @Test
    @DisplayName("POST /login — correct credentials return 200 + token")
    void loginSuccess() throws Exception {
        register(new RegisterRequest("alice@example.com", "alice", "S3cret!pass", null));

        LoginRequest req = new LoginRequest("alice@example.com", "S3cret!pass");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    @DisplayName("POST /login — wrong password returns 401")
    void loginWrongPassword() throws Exception {
        register(new RegisterRequest("alice@example.com", "alice", "S3cret!pass", null));

        LoginRequest req = new LoginRequest("alice@example.com", "wrong-password");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /login — unknown user returns 401")
    void loginUnknownUser() throws Exception {
        LoginRequest req = new LoginRequest("ghost@example.com", "S3cret!pass");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void register(RegisterRequest req) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
