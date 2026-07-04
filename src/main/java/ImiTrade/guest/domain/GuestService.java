package ImiTrade.guest.domain;

import ImiTrade.guest.dto.GuestResponse;
import ImiTrade.user.domain.User;
import ImiTrade.user.domain.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for guest lifecycle.
 *
 * <p>Delegates persistence to {@link UserService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestService {

    private final UserService userService;

    /**
     * Creates a new guest user with a random token and the guest starting balance.
     *
     * @return a {@link GuestResponse} containing the token and balance
     */
    @Transactional
    public GuestResponse createGuest() {
        User guest = userService.createGuest();
        return new GuestResponse(guest.getGuestToken(), guest.getBalance());
    }
}
