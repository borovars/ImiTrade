package ImiTrade.guest;

import ImiTrade.guest.dto.GuestResponse;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the guest endpoint. Uses the real security layer
 * (no mocks) and an in-memory H2 database (PostgreSQL-compatibility mode).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GuestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /guest — success returns 201 + token + balance, persists guest user")
    void createGuestSuccess() throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/guest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guestToken").isNotEmpty())
                .andExpect(jsonPath("$.balance").value(100000.0))
                .andReturn().getResponse().getContentAsString();

        GuestResponse response = objectMapper.readValue(responseBody, GuestResponse.class);
        assertThat(response.guestToken()).isNotNull();
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("100000.0000"));

        User saved = userRepository.findByGuestToken(response.guestToken()).orElseThrow();
        assertThat(saved.getIsGuest()).isTrue();
        assertThat(saved.getBalance()).isEqualByComparingTo(new BigDecimal("100000.0000"));
        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getUsername()).isNull();
        assertThat(saved.getPasswordHash()).isNull();
    }

    @Test
    @DisplayName("POST /guest — multiple calls create different tokens")
    void createGuestMultipleTimes() throws Exception {
        String body1 = mockMvc.perform(post("/api/v1/guest"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String body2 = mockMvc.perform(post("/api/v1/guest"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID token1 = objectMapper.readValue(body1, GuestResponse.class).guestToken();
        UUID token2 = objectMapper.readValue(body2, GuestResponse.class).guestToken();

        assertThat(token1).isNotEqualTo(token2);
    }
}
