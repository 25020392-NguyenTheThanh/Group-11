import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Electronics;
import com.auction.model.item.Art;
import com.auction.model.item.Vehicle;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.pattern.factory.ElectronicsFactory;
import com.auction.pattern.factory.ArtFactory;
import com.auction.pattern.factory.VehicleFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.auction.data.DataManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionSystemTest {

    @BeforeAll
    static void setUpAll() {
        DataManager.setTestMode(true);
    }

    // valid bid : kiểm tra đấu giá hợp lệ
    @Test
    void validBidShouldUpdateHighestPrice(){
        Item item = new Art(1, 123, "Mona Lisa", "Classic painting", 1000, "", "Leonardo da Vinci");

        // Bắt đầu từ bây giờ, kết thúc sau 1 ngày
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(1);

        Auction auction = new Auction(1, item , startTime , endTime , 20);
        Bidder bidder = new Bidder(1,"Tuan","123","123@gmail",2000, null);
        bidder.setAuthenticated(true);
        auction.setStatus(AuctionStatus.RUNNING);
        auction.placeBid(bidder ,1500);
        assertEquals(1500,auction.getCurrentHighestBid());
    }

    // invalid bid : kiểm tra đấu giá không hợp lệ
    @Test
    void invalidBidShouldThrowException(){
        Item item = new Vehicle(1, 123, "Tesla Model Y", "Electric SUV", 2000, "", 2023);
        // Chọn thời gian kiểm thử tường minh cố định tương lai
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 25, 10, 30);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 30, 10, 30);

        Auction auction = new Auction(1, item , startTime , endTime , 20);
        Bidder bidder = new Bidder(1,"Tuan","123","123@gmail",2000, null);
        bidder.setAuthenticated(true);
        auction.setStatus(AuctionStatus.RUNNING);
        assertThrows(InvalidBidException.class , () -> auction.placeBid(bidder , 1000));
    }

    // closed auction : kiểm tra khi auction đã đóng -> không cho bid nữa
    @Test
    void closedAuctionShouldRejectBid(){
        Item item = new Electronics(1 , 123,"Iphone","abc" ,2000 , "", "apple");

        // Bắt đầu cách đây 2 ngày, kết thúc cách đây 1 ngày
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now().plusDays(1);

        Auction auction = new Auction(1, item , startTime, endTime, 100);
        Bidder bidder = new Bidder(1,"Tuan","123","123@gmail",2000, null);
        bidder.setAuthenticated(true);
        auction.setStatus(AuctionStatus.FINISHED);
        assertThrows(AuctionClosedException.class,() -> auction.placeBid(bidder, 3000));
    }

    // concurrency test
    @Test
    void concurrentBiddingShouldKeepHighestPrice() throws Exception {
        Item item = new Electronics(1 , 123,"Iphone","abc" ,1000 , "", "apple");

        // SỬA: Set thời gian hợp lệ cho phiên trực tuyến kéo dài từ bây giờ tới ngày mai
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(1);

        Auction auction = new Auction(1, item , startTime , endTime ,100);
        Bidder bidder_1 = new Bidder(1,"Tuan","123","123@gmail",3000, null);
        Bidder bidder_2 = new Bidder(2,"Tuan_2","1234","1234@gmail",4000, null);
        bidder_1.setAuthenticated(true);
        bidder_2.setAuthenticated(true);
        auction.setStatus(AuctionStatus.RUNNING);
        Thread t1 = new Thread(() -> {
            auction.placeBid(bidder_1 , 2000);
        });
        Thread t2 = new Thread(() -> {
            auction.placeBid(bidder_2 , 3000);
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertEquals(3000 , auction.getCurrentHighestBid());
        assertEquals(bidder_2 , auction.getCurrentWinner());
    }

    // factory test
    @Test
    void factoryShouldCreateElectronicItem(){
        ElectronicsFactory electronicsfactory = new ElectronicsFactory("apple");
        Item item = electronicsfactory.createItem(1 , 123,"Iphone","abc" ,1000, "" );
        assertNotNull(item);
        assertEquals("ELECTRONICS" , item.getCategory());
    }

    @Test
    void factoryShouldCreateArtItem(){
        ArtFactory artFactory = new ArtFactory("Leonardo da Vinci");
        Item item = artFactory.createItem(2, 123, "Mona Lisa", "Classic painting", 1000, "");
        assertNotNull(item);
        assertEquals("ART", item.getCategory());
        assertTrue(item instanceof Art);
        assertEquals("Leonardo da Vinci", ((Art)item).getArtist());
    }

    @Test
    void factoryShouldCreateVehicleItem(){
        VehicleFactory vehicleFactory = new VehicleFactory(2023);
        Item item = vehicleFactory.createItem(3, 123, "Tesla Model Y", "Electric SUV", 2000, "");
        assertNotNull(item);
        assertEquals("VEHICLE", item.getCategory());
        assertTrue(item instanceof Vehicle);
        assertEquals(2023, ((Vehicle)item).getYear());
    }

    // auction init
    @Test
    void auctionShouldInitializeCorrectly(){
        Item item = new Electronics(1,123, "Iphone","abc",1000, "", "apple");

        // Cung cấp đủ cặp ngày bắt đầu và kết thúc
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(1);

        Auction auction = new Auction(1 , item , startTime , endTime , 100);
        assertEquals(1000 , auction.getCurrentHighestBid());
        assertEquals(AuctionStatus.OPEN , auction.getStatus());
    }
    @Test
    void antiSnipingShouldExtendEndTime() {
        Item item = new Electronics(1, 123, "Iphone", "abc", 1000, "", "apple");
        LocalDateTime start = LocalDateTime.now();
        // endTime chỉ còn 10 giây — nằm trong snipe window 30s
        LocalDateTime end = LocalDateTime.now().plusSeconds(10);
        Auction auction = new Auction(1, item, start, end, 100);
        Bidder bidder = new Bidder(1, "Tuan", "123", "123@gmail", 5000, null);
        bidder.setAuthenticated(true);
        auction.setStatus(AuctionStatus.RUNNING);

        LocalDateTime endBefore = auction.getEndTime();
        auction.placeBid(bidder, 2000);
        // endTime phải được gia hạn
        assertTrue(auction.getEndTime().isAfter(endBefore));
    }

    @Test
    void autoBidShouldRegisterConfig() {
        Item item = new Electronics(1, 123, "Iphone", "abc", 1000, "", "apple");
        Auction auction = new Auction(1, item,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 100);
        auction.setStatus(AuctionStatus.RUNNING);

        com.auction.model.auction.AutoBidConfig cfg =
                new com.auction.model.auction.AutoBidConfig(99, "testUser", 5000, 200);
        auction.registerAutoBid(cfg);

        assertEquals(1, auction.getAutoBidConfigs().size());
        assertEquals(5000, auction.getAutoBidConfigs().get(99).getMaxBid());
    }

    @Test
    void autoBidShouldAutomaticallyPlaceBid() {
        Item item = new Electronics(1, 123, "Iphone", "abc", 1000, "", "apple");
        Auction auction = new Auction(1, item,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 100);
        auction.setStatus(AuctionStatus.RUNNING);

        // Bidder 1 (Auto-bidder): maxBid = 2000, increment = 150
        Bidder bidder1 = new Bidder(1, "AutoBidder", "123", "autobid@gmail.com", 5000, null);
        bidder1.setAuthenticated(true);
        auction.addObserver(bidder1);

        com.auction.model.auction.AutoBidConfig cfg =
                new com.auction.model.auction.AutoBidConfig(1, "AutoBidder", 2000, 150);
        auction.registerAutoBid(cfg);

        // Bidder 2 (Manual bidder): places a manual bid of 1300
        Bidder bidder2 = new Bidder(2, "ManualBidder", "123", "manual@gmail.com", 5000, null);
        bidder2.setAuthenticated(true);
        auction.addObserver(bidder2);

        auction.placeBid(bidder2, 1300);

        assertEquals(1450.0, auction.getCurrentHighestBid());
        assertEquals("AutoBidder", auction.getCurrentWinner().getUsername());
        assertEquals(3550.0, bidder1.getBalance()); // 5000 - 1450
        assertEquals(5000.0, bidder2.getBalance()); // Refunded 1300
    }

    @Test
    void autoBidShouldPersistAfterLogout() {
        Item item = new Electronics(1, 123, "Iphone", "abc", 1000, "", "apple");
        Auction auction = new Auction(1, item,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 100);
        auction.setStatus(AuctionStatus.RUNNING);

        // Bidder 1 (Auto-bidder)
        Bidder bidder1 = new Bidder(1, "AutoBidder", "123", "autobid@gmail.com", 5000, null);
        bidder1.setAuthenticated(true);
        auction.addObserver(bidder1);

        com.auction.model.auction.AutoBidConfig cfg =
                new com.auction.model.auction.AutoBidConfig(1, "AutoBidder", 2000, 150);
        auction.registerAutoBid(cfg);

        // Bidder 1 logs out (authenticated becomes false)
        bidder1.logout();
        assertFalse(bidder1.isAuthenticated());

        // Bidder 2 (Manual bidder) places a manual bid of 1300
        Bidder bidder2 = new Bidder(2, "ManualBidder", "123", "manual@gmail.com", 5000, null);
        bidder2.setAuthenticated(true);
        auction.addObserver(bidder2);

        // The auto-bid should still be triggered and succeed despite bidder1 being logged out
        auction.placeBid(bidder2, 1300);

        assertEquals(1450.0, auction.getCurrentHighestBid());
        assertEquals("AutoBidder", auction.getCurrentWinner().getUsername());
    }
}
