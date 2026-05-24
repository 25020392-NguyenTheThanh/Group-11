package com.auction.server;

import com.auction.data.DataManager;
import com.auction.manager.AuctionManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionTimer {
    private final AuctionServer server ;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final java.util.Set<Integer> warned5MinAuctions = new java.util.HashSet<>();
    private final java.util.Set<Integer> warned1MinAuctions = new java.util.HashSet<>();

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
            // Tự động kích hoạt phiên OPEN đã đến giờ bắt đầu
            if (auction.getStatus() == AuctionStatus.OPEN) {
                LocalDateTime now = LocalDateTime.now();
                if (auction.getStartTime() == null || !auction.getStartTime().isAfter(now)) {
                    try {
                        auction.start();
                        DataManager.getInstance().startAuction(auction.getId());

                        // Phát thông báo realtime cho tất cả client
                        String msg = "Phiên #" + auction.getId() + " [" + auction.getItem().getName() + "] đã bắt đầu!";
                        server.broadcast(new Notification("AUCTION_ENDED", msg));
                        server.broadcast(new Notification("ITEM_STATUS_CHANGED", String.valueOf(auction.getItem().getId())));
                        System.out.println("Timer kích hoạt phiên #" + auction.getId() + " sang RUNNING.");

                        // Gửi thông báo đến những người đã watchlist phiên này
                        for (ClientHandler client : server.getConnectedClients()) {
                            User u = client.getLoggedInUser();
                            if (u instanceof Bidder bidder) {
                                if (bidder.getProfile().getWatchlist().contains(auction.getId())) {
                                    client.sendNotification(new Notification("WATCHLIST_STARTED", String.format("Sản phẩm [%s] trong danh sách quan tâm của bạn đã lên sàn đấu giá!", auction.getItem().getName())));
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi khi tự động kích hoạt phiên #" + auction.getId() + ": " + e.getMessage());
                    }
                }
            }

            // Kiểm tra phiên sắp kết thúc (dưới 5 phút, dưới 1 phút)
            if (auction.getStatus() == AuctionStatus.RUNNING) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(auction.getEndTime())) {
                    long secondsRemaining = java.time.Duration.between(now, auction.getEndTime()).toSeconds();
                    if (secondsRemaining > 0 && secondsRemaining < 60) { // dưới 1 phút
                        if (!warned1MinAuctions.contains(auction.getId())) {
                            warned1MinAuctions.add(auction.getId());
                            sendEndingSoonNotification(auction, 1);
                        }
                    } else if (secondsRemaining > 0 && secondsRemaining < 300) { // dưới 5 phút
                        if (!warned5MinAuctions.contains(auction.getId())) {
                            warned5MinAuctions.add(auction.getId());
                            sendEndingSoonNotification(auction, 5);
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
                        // Persist bidder won auction
                        DataManager.getInstance().saveBidderWon(
                                auction.getCurrentWinner().getId(),
                                auction.getId()
                        );
                        // Update in-memory profile of the winner on the server
                        auction.getCurrentWinner().getProfile().addWonAuction(auction.getId());
                    }

                    // Gửi thông báo kết quả đấu giá
                    if (auction.getCurrentWinner() != null) {
                        Bidder winner = auction.getCurrentWinner();
                        for (ClientHandler client : server.getConnectedClients()) {
                            User u = client.getLoggedInUser();
                            if (u != null) {
                                if (u.getId() == winner.getId()) {
                                    client.sendNotification(new Notification("AUCTION_WON", String.format("Chúc mừng! Bạn đã thắng phiên đấu giá #%d [%s] với số tiền $%,.2f. Vui lòng nhấn vào thông báo này hoặc chuyển tới Lịch sử để thanh toán!", auction.getId(), auction.getItem().getName(), auction.getCurrentHighestBid())));
                                } else if (u instanceof Bidder bidder) {
                                    // Kiểm tra xem bidder này có tham gia đấu giá phiên này không
                                    boolean hasBid = false;
                                    for (com.auction.model.auction.BidTransaction tx : auction.getBidHistory()) {
                                        if (tx.getBidderId() == bidder.getId()) {
                                            hasBid = true;
                                            break;
                                        }
                                    }
                                    if (hasBid) {
                                        client.sendNotification(new Notification("AUCTION_LOST", String.format("Phiên đấu giá #%d [%s] đã kết thúc. Rất tiếc, bạn đã không phải người trả giá cao nhất (giá thắng cuộc: $%,.2f).", auction.getId(), auction.getItem().getName(), auction.getCurrentHighestBid())));
                                    }
                                } else if (u.getId() == auction.getItem().getOwnerId()) {
                                    // Thông báo cho Seller
                                    client.sendNotification(new Notification("SELLER_AUCTION_SUCCESS", String.format("Phiên đấu giá sản phẩm [%s] của bạn đã kết thúc thành công! Giá thắng cuộc: $%,.2f. Người thắng: @%s.", auction.getItem().getName(), auction.getCurrentHighestBid(), winner.getUsername())));
                                }
                            }
                        }
                    } else {
                        // Thất bại
                        for (ClientHandler client : server.getConnectedClients()) {
                            User u = client.getLoggedInUser();
                            if (u != null) {
                                if (u.getId() == auction.getItem().getOwnerId()) {
                                    client.sendNotification(new Notification("SELLER_AUCTION_FAILED", String.format("Phiên đấu giá sản phẩm [%s] của bạn đã kết thúc nhưng không có người trả giá.", auction.getItem().getName())));
                                } else if (u instanceof Bidder bidder) {
                                    if (bidder.getProfile().getWatchlist().contains(auction.getId())) {
                                        client.sendNotification(new Notification("AUCTION_LOST", String.format("Phiên đấu giá #%d [%s] đã kết thúc mà không có người đặt giá.", auction.getId(), auction.getItem().getName())));
                                    }
                                }
                            }
                        }
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
        for (Auction auction : AuctionManager.getInstance().getAuctions()) {
            if (auction.getStatus() != AuctionStatus.RUNNING) continue;

            auction.getAutoBidConfigs().forEach((bidderId, cfg) -> {
                double nextBid = auction.getCurrentHighestBid() + cfg.getIncrement();

                // Không trigger nếu vượt ngưỡng tối đa
                if (nextBid > cfg.getMaxBid()) return;

                // Không trigger nếu người có config đang là winner rồi
                if (auction.getCurrentWinner() != null &&
                        auction.getCurrentWinner().getId() == bidderId) return;

                com.auction.model.user.User u =
                        com.auction.manager.UserManager.getInstance().findUserById(bidderId);
                if (!(u instanceof com.auction.model.user.Bidder bidder)) return;

                try {
                    auction.placeBid(bidder, nextBid);
                    AuctionManager.getInstance().recordBid(
                            auction.getId(), bidderId, bidder.getUsername(), nextBid);
                    server.broadcast(new Notification("BID_UPDATE",
                            "Auto-bid: " + bidder.getUsername() + " đặt " + nextBid + "₫"));
                } catch (Exception e) {
                    System.err.println("[AutoBid] lỗi phiên " + auction.getId() + ": " + e.getMessage());
                }
            });
        }
    }

    private void sendEndingSoonNotification(Auction auction, int minutes) {
        String msg = String.format("Phiên đấu giá #%d [%s] sắp kết thúc (còn dưới %d phút)!", auction.getId(), auction.getItem().getName(), minutes);
        Notification notification = new Notification("ENDING_SOON", msg);
        
        for (ClientHandler client : server.getConnectedClients()) {
            User u = client.getLoggedInUser();
            if (u instanceof Bidder bidder) {
                boolean isWatched = bidder.getProfile().getWatchlist().contains(auction.getId());
                boolean isParticipant = false;
                for (com.auction.model.auction.BidTransaction tx : auction.getBidHistory()) {
                    if (tx.getBidderId() == bidder.getId()) {
                        isParticipant = true;
                        break;
                    }
                }
                if (isWatched || isParticipant) {
                    client.sendNotification(notification);
                }
            }
        }
    }

    public void stop(){
        scheduler.shutdown();
    }
}
