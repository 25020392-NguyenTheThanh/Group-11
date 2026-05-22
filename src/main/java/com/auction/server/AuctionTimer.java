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
    private final java.util.Set<Integer> warnedAuctions = new java.util.HashSet<>();

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
            // Kiểm tra phiên sắp kết thúc (dưới 5 phút)
            if (auction.getStatus() == AuctionStatus.RUNNING) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(auction.getEndTime())) {
                    long secondsRemaining = java.time.Duration.between(now, auction.getEndTime()).toSeconds();
                    if (secondsRemaining > 0 && secondsRemaining < 300) { // dưới 5 phút
                        if (!warnedAuctions.contains(auction.getId())) {
                            warnedAuctions.add(auction.getId());
                            sendEndingSoonNotification(auction);
                        }
                    }
                }
            }

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
                        // Persist bidder won auction
                        DataManager.getInstance().saveBidderWon(
                                auction.getCurrentWinner().getId(),
                                auction.getId()
                        );
                        // Update in-memory profile of the winner on the server
                        auction.getCurrentWinner().getProfile().addWonAuction(auction.getId());
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

    private void sendEndingSoonNotification(Auction auction) {
        String msg = "Phiên đấu giá #" + auction.getId() + " [" + auction.getItem().getName() + "] sắp kết thúc (còn dưới 5 phút)!";
        Notification notification = new Notification("WATCHLIST_ENDING_SOON", msg);
        
        for (ClientHandler client : server.getConnectedClients()) {
            com.auction.model.user.User u = client.getLoggedInUser();
            if (u instanceof com.auction.model.user.Bidder bidder) {
                if (bidder.getProfile().getWatchlist().contains(auction.getId())) {
                    client.sendNotification(notification);
                }
            }
        }
    }

    public void stop(){
        scheduler.shutdown();
    }
}
