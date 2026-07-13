package ImiTrade.guest.domain;

import ImiTrade.guest.dto.GuestResponse;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private GuestService guestService;

    @DisplayName("createGuest: returns token and initial balance 5000")
    @Test
    void createGuestSuccess() {
        UUID token = UUID.randomUUID();
        User guest = User.builder()
                .id(1L)
                .balance(new BigDecimal("5000.0000"))
                .isGuest(true)
                .guestToken(token)
                .build();
        when(userService.createGuest()).thenReturn(guest);

        GuestResponse response = guestService.createGuest();

        assertThat(response.guestToken()).isEqualTo(token);
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("5000.0000"));
    }
}
