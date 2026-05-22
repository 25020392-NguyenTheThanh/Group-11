package com.auction.server;

import com.auction.data.DataManager;
import com.auction.manager.AuctionManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.network.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionTimer {
    private final AuctionServer server ;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    // threadpool chạy task theo thời gian
    public AuctionTimer(AuctionServer server){
        this.server = server ;
    }

    public void start(){
        // Cứ 10 giây quét 1 lần — kiểm tra phiên nào đã hết giờ
        scheduler.scheduleAtFixedRate(() -> checkExpiredAuctions() , 0 , 10 , TimeUnit.SECONDS);
        System.out.println("AuctionTimer đã khởi động - quét mỗi 10 giây");
    }

    private void checkExpiredAuctions(){
        List<Auction> auctions = AuctionManager.getInstance().getAuctions();
        for (Auction auction : auctions){
            // Chỉ xử lý phiên đang RUNNING và đã hết giờ
            if (auction.getStatus() == AuctionStatus.RUNNING && LocalDateTime.now().isAfter(auction.getEndTime())){
                try {
                    auction.finish(); // chuyển sang FINISHED hoặc CANCELLED
                    AuctionManager.getInstance().finishAuction(auction.getId());
                    
                    com.auction.model.item.ItemStatus newStatus = (auction.getCurrentWinner() != null)
                            ? com.auction.model.item.ItemStatus.SOLD
                            : com.auction.model.item.ItemStatus.UNSOLD;
                    com.auction.manager.ItemManager.getInstance().updateItemStatus(auction.getItem().getId(), newStatus);

                    if (auction.getCurrentWinner() != null) {
                        DataManager.getInstance().updateSellerRevenue(
                                auction.getItem().getOwnerId(),
                                auction.getCurrentHighestBid()
                        );
                    }

                    // thông báo cho tất cả client
                    String msg = "Phiên " + auction.getId() + " [" + auction.getItem().getName() + "] đã kết thúc" ;
                    server.broadcast(new Notification("AUCTION_ENDED" , msg));
                    server.broadcast(new Notification("ITEM_STATUS_CHANGED", String.valueOf(auction.getItem().getId())));
                    System.out.println("Timer kết thúc phiên " + auction.getId() + ", cập nhật trạng thái sản phẩm thành " + newStatus);
                } catch(Exception e){
                    System.err.println("Lỗi khi kết thúc ở phiên " + auction.getId() + ": " + e.getMessage());
                }
            }
        }
    }
    public void stop(){
        scheduler.shutdown();
    }
}
