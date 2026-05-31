package com.auction.model.auction;

import com.auction.data.DataManager;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.manager.AuctionManager;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.BidUpdateData;
import com.auction.network.Notification;
import com.auction.pattern.observer.Observer;
import com.auction.server.AuctionServer;
import com.auction.server.ClientHandler;

import java.time.LocalDateTime;

class BidProcessor {

    private final Auction auction;

    BidProcessor(Auction auction) {
        this.auction = auction;
    }

    synchronized void execute(Bidder bidder, double amount, boolean isAutoBid)
            throws InvalidBidException, AuctionClosedException {

        if (amount <= 0)
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");

        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            if (auction.getStatus() == AuctionStatus.RUNNING) auction.finish();
            throw new AuctionClosedException("Phiên #" + auction.getId() + " đã hết giờ.");
        }

        if (auction.getStatus() != AuctionStatus.RUNNING)
            throw new AuctionClosedException(String.format(
                    "Phiên #%d không ở trạng thái RUNNING (hiện: %s)", auction.getId(), auction.getStatus()));

        if (auction.getCurrentWinner() != null && auction.getCurrentWinner().getId() == bidder.getId())
            throw new InvalidBidException("Bạn đã là người đặt giá cao nhất, không thể đặt giá tiếp.");

        if (bidder.getBalance() < amount)
            throw new InvalidBidException(String.format(
                    "Số dư không đủ — số dư hiện tại: $%,.0f, giá đặt: $%,.0f", bidder.getBalance(), amount));

        double minAccepted = auction.getCurrentHighestBid() + auction.getMinBidStep();
        if (amount < minAccepted)
            throw new InvalidBidException(String.format(
                    "Giá đặt phải >= $%,.0f  (giá cao nhất $%,.0f + bước tối thiểu $%,.0f)",
                    minAccepted, auction.getCurrentHighestBid(), auction.getMinBidStep()));

        // Trừ tiền bidder mới
        if (DataManager.isTestMode()) {
            bidder.setBalance(bidder.getBalance() - amount);
        } else {
            boolean success = DataManager.getInstance().deductBidderBalance(bidder.getId(), amount);
            if (!success)
                throw new InvalidBidException(String.format("Số dư không đủ hoặc lỗi hệ thống (Cần $%,.0f)", amount));
            bidder.setBalance(DataManager.getInstance().getBidderBalance(bidder.getId()));
        }

        // Hoàn tiền bidder cũ
        Bidder previousWinner = auction.getCurrentWinner();
        double previousHighestBid = auction.getCurrentHighestBid();
        if (previousWinner != null) {
            if (DataManager.isTestMode()) {
                previousWinner.setBalance(previousWinner.getBalance() + previousHighestBid);
            } else {
                DataManager.getInstance().addBidderBalance(previousWinner.getId(), previousHighestBid);
                previousWinner.setBalance(DataManager.getInstance().getBidderBalance(previousWinner.getId()));
            }
        }

        String bidType = isAutoBid ? "AUTO" : "MANUAL";
        auction.getBidHistory().add(new BidTransaction(bidder.getId(), bidder.getUsername(), amount, bidType));
        auction.setCurrentHighestBid(amount);
        auction.setCurrentWinner(bidder);

        if (bidder.getProfile() != null) bidder.getProfile().addParticipatedAuction(auction.getId());

        try {
            AuctionManager.getInstance().recordBid(auction.getId(), bidder.getId(), bidder.getUsername(), amount, bidType);
        } catch (Exception e) {
            System.err.println("Lỗi ghi lịch sử MySQL: " + e.getMessage());
        }

        new AntiSnipeGuard(auction).checkAndExtend();

        // Thông báo room
        String msg = isAutoBid
                ? String.format("🔔 Phiên %d — Auto-Bid: %s vừa đặt $%,.2f | Giá cao nhất hiện tại: $%,.2f", auction.getId(), bidder.getUsername(), amount, auction.getCurrentHighestBid())
                : String.format("🔔 Phiên %d — %s vừa đặt $%,.2f | Giá cao nhất hiện tại: $%,.2f", auction.getId(), bidder.getUsername(), amount, auction.getCurrentHighestBid());
        System.out.println(msg);

        BidUpdateData upd = new BidUpdateData(auction.getId(), auction.getCurrentHighestBid(), bidder.getUsername(), auction.getBidHistory().size(), auction.getEndTime());
        for (Observer o : auction.getObservers()) {
            if (o instanceof ClientHandler ch) ch.sendNotification(new Notification("BID_UPDATE", upd));
            else o.send(msg);
        }
        auction.notifyObservers(msg);

        // Thông báo bidder vừa thắng
        if (AuctionServer.getInstance() != null) {
            for (ClientHandler client : AuctionServer.getInstance().getConnectedClients()) {
                User u = client.getLoggedInUser();
                if (u != null && u.getId() == bidder.getId()) {
                    client.sendNotification(new Notification("BALANCE_UPDATE", bidder.getBalance()));
                    String successMsg = isAutoBid
                            ? String.format("Auto-Bid đặt giá thành công $%,.2f cho phiên #%d [%s].", amount, auction.getId(), auction.getItem().getName())
                            : String.format("Đặt giá thành công $%,.2f cho phiên #%d [%s].", amount, auction.getId(), auction.getItem().getName());
                    client.sendNotification(new Notification("BID_SUCCESS", successMsg));
                    break;
                }
            }
        }

        // Thông báo bidder bị vượt
        if (previousWinner != null && previousWinner.getId() != bidder.getId() && AuctionServer.getInstance() != null) {
            for (ClientHandler client : AuctionServer.getInstance().getConnectedClients()) {
                User u = client.getLoggedInUser();
                if (u != null && u.getId() == previousWinner.getId() && u instanceof Bidder b) {
                    b.setBalance(previousWinner.getBalance());
                    client.sendNotification(new Notification("BALANCE_UPDATE", b.getBalance()));
                    client.sendNotification(new Notification("OUTBID", String.format(
                            "Bạn đã bị vượt giá ở phiên #%d [%s]! Giá cao nhất hiện tại là $%,.2f.", auction.getId(), auction.getItem().getName(), amount)));
                    break;
                }
            }
        }

        // Thông báo seller
        if (AuctionServer.getInstance() != null) {
            for (ClientHandler client : AuctionServer.getInstance().getConnectedClients()) {
                User u = client.getLoggedInUser();
                if (u != null && u.getId() == auction.getItem().getOwnerId()) {
                    if (auction.getBidHistory().size() == 1)
                        client.sendNotification(new Notification("SELLER_FIRST_BID", String.format(
                                "Đã có người đặt giá đầu tiên cho sản phẩm [%s] của bạn: $%,.2f.", auction.getItem().getName(), amount)));
                    if (auction.getBidHistory().size() % 5 == 0)
                        client.sendNotification(new Notification("SELLER_BID_SURGE", String.format(
                                "Sản phẩm [%s] của bạn đang thu hút sự quan tâm với %d lượt đặt giá!", auction.getItem().getName(), auction.getBidHistory().size())));
                    if (previousHighestBid < auction.getItem().getStartingPrice() * 2 && amount >= auction.getItem().getStartingPrice() * 2)
                        client.sendNotification(new Notification("SELLER_PRICE_MILESTONE", String.format(
                                "Sản phẩm [%s] của bạn đã vượt mốc giá gấp đôi giá khởi điểm: $%,.2f!", auction.getItem().getName(), amount)));
                    break;
                }
            }
        }

        // Broadcast toàn server
        if (AuctionServer.getInstance() != null)
            AuctionServer.getInstance().broadcast(new Notification("BID_UPDATE",
                    new BidUpdateData(auction.getId(), auction.getCurrentHighestBid(), bidder.getUsername(), auction.getBidHistory().size(), auction.getEndTime())));
    }
}