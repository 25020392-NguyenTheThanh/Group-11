import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.pattern.factory.ElectronicsFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionSystemTest {

    // valid bid : kiểm tra đấu giá hợp lệ
    @Test
    void validBidShouldUpdateHighestPrice(){
        Item item = new Electronics(1 , 123,"Iphone","abc" ,1000 , "", "apple");

        // Bắt đầu từ bây giờ, kết thúc sau 1 ngày
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(1);

        Auction auction = new Auction(1, item , startTime , endTime , 20);
        Bidder bidder = new Bidder(1,"Tuan","123","123@gmail",2000);
        bidder.setAuthenticated(true);
        auction.setStatus(AuctionStatus.RUNNING);
        auction.placeBid(bidder ,1500);
        assertEquals(1500,auction.getCurrentHighestBid());
    }

    // invalid bid : kiểm tra đấu giá không hợp lệ
    @Test
    void invalidBidShouldThrowException(){
        Item item = new Electronics(1 , 123,"Iphone","abc" ,2000 , "", "apple");
        // Chọn thời gian kiểm thử tường minh cố định tương lai
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 25, 10, 30);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 30, 10, 30);

        Auction auction = new Auction(1, item , startTime , endTime , 20);
        Bidder bidder = new Bidder(1,"Tuan","123","123@gmail",2000);
        bidder.setAuthenticated(true);
        auction.setStatus(AuctionStatus.RUNNING);
        assertThrows(InvalidBidException.class , () -> auction.placeBid(bidder , 1000));
    }

    // closed auction : kiểm tra khi auction đã đóng -> không cho bid nữa
    @Test
    void closedAuctionShouldRejectBid(){
        Item item = new Electronics(1 , 123,"Iphone","abc" ,2000 , "", "apple");

        // Bắt đầu cách đây 2 ngày, kết thúc cách đây 1 ngày
        LocalDateTime startTime = LocalDateTime.now().minusDays(2);
        LocalDateTime endTime = LocalDateTime.now().minusDays(1);

        Auction auction = new Auction(1, item , startTime, endTime, 100);
        Bidder bidder = new Bidder(1,"Tuan","123","123@gmail",2000);
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
        Bidder bidder_1 = new Bidder(1,"Tuan","123","123@gmail",3000);
        Bidder bidder_2 = new Bidder(2,"Tuan_2","1234","1234@gmail",4000);
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
}
