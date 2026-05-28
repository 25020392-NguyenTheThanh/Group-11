import com.auction.data.DataManager;
import com.auction.exception.AuthenticationException;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.model.user.Seller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserManagerTest {

    @BeforeAll
    static void init() {
        DataManager.setTestMode(true);
    }

    @Test
    void bidderShouldLoginAndLogout() {
        Bidder bidder = new Bidder(1, "alice", "pass123", "alice@test.com", 1000, null);
        bidder.setAuthenticated(true);
        assertTrue(bidder.isAuthenticated());
        bidder.logout();
        assertFalse(bidder.isAuthenticated());
    }

    @Test
    void loginWithWrongPasswordShouldThrow() {
        Bidder bidder = new Bidder(1, "alice", "correct_hash", "alice@test.com", 1000, null);
        // login() dùng PasswordUtil.verify — plain text sai sẽ throw
        assertThrows(AuthenticationException.class, () -> bidder.login("wrong_password"));
    }

    @Test
    void sellerRoleShouldBeSELLER() {
        Seller seller = new Seller(2, "bob", "pass", "bob@test.com");
        assertEquals("SELLER", seller.getRole());
    }

    @Test
    void bidderRoleShouldBeBIDDER() {
        Bidder bidder = new Bidder(3, "carol", "pass", "carol@test.com", 500, null);
        assertEquals("BIDDER", bidder.getRole());
    }

    @Test
    void banShouldDeactivateUser() {
        Bidder bidder = new Bidder(4, "dave", "pass", "dave@test.com", 0, null);
        assertTrue(bidder.isActive());
        bidder.setActive(false);
        bidder.setBanReason("Vi phạm quy định");
        assertFalse(bidder.isActive());
        assertEquals("Vi phạm quy định", bidder.getBanReason());
    }

    @Test
    void balanceShouldDeductCorrectly() {
        Bidder bidder = new Bidder(5, "eve", "pass", "eve@test.com", 5000, null);
        bidder.setBalance(bidder.getBalance() - 1500);
        assertEquals(3500, bidder.getBalance(), 0.01);
    }

    @Test
    void balanceShouldRefundCorrectly() {
        Bidder bidder = new Bidder(6, "frank", "pass", "frank@test.com", 2000, null);
        bidder.setBalance(bidder.getBalance() + 800);
        assertEquals(2800, bidder.getBalance(), 0.01);
    }
}