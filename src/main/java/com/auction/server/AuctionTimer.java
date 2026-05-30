package com.auction.server;

import com.auction.data.DataManager;
import com.auction.manager.AuctionManager;
import com.auction.manager.ItemManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.ItemStatus;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.Notification;

import java.time.Duration;
import java.time.LocalDateTime;
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

    public void stop(){
        scheduler.shutdown();
    }

    // Xử lý từng phiên theo trạng thái
    private void checkExpiredAuctions(){
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : AuctionManager.getInstance().getAuctions()){
            switch (auction.getStatus()){
                case OPEN -> tryActivateAuction(auction , now);
                case RUNNING -> checkRunningAuction(auction , now);
                default -> {}
            }
        }
    }

    // OPEN : kích hoạt phiên khi đến giờ bắt đầu
    private void tryActivateAuction(Auction auction, LocalDateTime now) {
        if (auction.getStartTime() != null && auction.getStartTime().isAfter(now)) return;
        try {
            auction.start();
            DataManager.getInstance().startAuction(auction.getId());

            server.broadcast(new Notification("AUCTION_STARTED",
                    "Phiên #" + auction.getId() + " [" + auction.getItem().getName() + "] đã bắt đầu!"));
            server.broadcast(new Notification("ITEM_STATUS_CHANGED", String.valueOf(auction.getItem().getId())));

            notifyWatchlistBidders(auction,
                    "WATCHLIST_STARTED",
                    String.format("Sản phẩm [%s] trong danh sách quan tâm của bạn đã lên sàn đấu giá!",
                            auction.getItem().getName()));

            System.out.println("Timer kích hoạt phiên #" + auction.getId() + " sang RUNNING.");
        } catch (Exception e) {
            System.err.println("Lỗi khi kích hoạt phiên #" + auction.getId() + ": " + e.getMessage());
        }
    }

    // RUNNING : cảnh báo sắp hết giờ , kết thúc khi đã đến giờ
    private void checkRunningAuction(Auction auction, LocalDateTime now) {
        if (now.isAfter(auction.getEndTime())) {
            tryFinishAuction(auction);
        } else {
            checkEndingSoonWarnings(auction, now);
        }
    }

    // cảnh báo nếu thời gian còn dưới 5 phút hoặc 1 phút
    private void checkEndingSoonWarnings(Auction auction, LocalDateTime now) {
        long secondsLeft = Duration.between(now, auction.getEndTime()).toSeconds();
        if (secondsLeft <= 0) return;

        if (secondsLeft < 60 && warned1MinAuctions.add(auction.getId())) {
            sendEndingSoonNotification(auction, 1);
        } else if (secondsLeft < 300 && warned5MinAuctions.add(auction.getId())) {
            sendEndingSoonNotification(auction, 5);
        }
    }

    // kết thúc phiên , cập nhật dữ liệu , gửi thông báo cho client
    private void tryFinishAuction(Auction auction) {
        try {
            auction.finish();
            AuctionManager.getInstance().finishAuction(auction.getId());

            ItemStatus newStatus = (auction.getCurrentWinner() != null) ? ItemStatus.SOLD : ItemStatus.UNSOLD;
            ItemManager.getInstance().updateItemStatus(auction.getItem().getId(), newStatus);

            persistWinnerIfPresent(auction);
            notifyAuctionResult(auction);

            server.broadcast(new Notification("AUCTION_ENDED",
                    "Phiên " + auction.getId() + " [" + auction.getItem().getName() + "] đã kết thúc"));
            server.broadcast(new Notification("ITEM_STATUS_CHANGED",
                    String.valueOf(auction.getItem().getId())));

            System.out.println("Timer kết thúc phiên #" + auction.getId() + ", item → " + newStatus);
        } catch (Exception e) {
            System.err.println("Lỗi khi kết thúc phiên #" + auction.getId() + ": " + e.getMessage());
        }
    }
    // xác định phiên đấu giá thành công hay thất bại
    private void notifyAuctionResult(Auction auction) {
        if (auction.getCurrentWinner() != null) {
            notifyAuctionSuccess(auction);
        } else {
            notifyAuctionFailed(auction);
        }
    }
    // gủi thông báo cho người thắng , người bán , người tham gia khi kết phiên thành công
    private void notifyAuctionSuccess(Auction auction) {
        Bidder winner = auction.getCurrentWinner();
        for (ClientHandler client : server.getConnectedClients()) {
            User u = client.getLoggedInUser();
            if (u == null) continue;

            if (u.getId() == winner.getId()) {
                client.sendNotification(new Notification("AUCTION_WON", String.format(
                        "Chúc mừng! Bạn đã thắng phiên #%d [%s] với $%,.2f. Vui lòng chuyển tới Lịch sử để thanh toán!",
                        auction.getId(), auction.getItem().getName(), auction.getCurrentHighestBid())));

            } else if (u.getId() == auction.getItem().getOwnerId()) {
                client.sendNotification(new Notification("SELLER_AUCTION_SUCCESS", String.format(
                        "Phiên [%s] kết thúc thành công! Giá thắng: $%,.2f. Người thắng: @%s.",
                        auction.getItem().getName(), auction.getCurrentHighestBid(), winner.getUsername())));

            } else if (u instanceof Bidder bidder && hasParticipated(bidder, auction)) {
                client.sendNotification(new Notification("AUCTION_LOST", String.format(
                        "Phiên #%d [%s] đã kết thúc. Rất tiếc, bạn không phải người trả giá cao nhất ($%,.2f).",
                        auction.getId(), auction.getItem().getName(), auction.getCurrentHighestBid())));
            }
        }
    }
    // gửi thông báo cho người theo dõi , người bán nếu phiên thất bại
    private void notifyAuctionFailed(Auction auction) {
        for (ClientHandler client : server.getConnectedClients()) {
            User u = client.getLoggedInUser();
            if (u == null) continue;

            if (u.getId() == auction.getItem().getOwnerId()) {
                client.sendNotification(new Notification("SELLER_AUCTION_FAILED", String.format(
                        "Phiên [%s] kết thúc nhưng không có người trả giá.",
                        auction.getItem().getName())));

            } else if (u instanceof Bidder bidder
                    && bidder.getProfile().getWatchlist().contains(auction.getId())) {
                client.sendNotification(new Notification("AUCTION_LOST", String.format(
                        "Phiên #%d [%s] kết thúc mà không có người đặt giá.",
                        auction.getId(), auction.getItem().getName())));
            }
        }
    }
    // gửi thông báo tới tất cả bidder đang theo dõi phiên đấu trong watchlist
    private void notifyWatchlistBidders(Auction auction, String type, String message) {
        for (ClientHandler client : server.getConnectedClients()) {
            if (client.getLoggedInUser() instanceof Bidder bidder
                    && bidder.getProfile().getWatchlist().contains(auction.getId())) {
                client.sendNotification(new Notification(type, message));
            }
        }
    }
    // kiểm tra bidder từng đặt giá trong phiên hay chưa
    private boolean hasParticipated(Bidder bidder, Auction auction) {
        for (BidTransaction tx : auction.getBidHistory()) {
            if (tx.getBidderId() == bidder.getId()) return true;
        }
        return false;
    }
    // lưu thông tin người thắng đấu giá vào cơ sở dữ liệu
    private void persistWinnerIfPresent(Auction auction) {
        Bidder winner = auction.getCurrentWinner();
        if (winner == null) return;
        DataManager.getInstance().saveBidderWon(winner.getId(), auction.getId());
        winner.getProfile().addWonAuction(auction.getId());
    }

    // gửi cảnh báo sắp kết thúc phiên cho người theo dõi hoặc đã tham gia đặt giá
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
}
