package com.auction.server.handler;

import com.auction.data.DataManager;
import com.auction.manager.AuctionManager;
import com.auction.manager.ItemManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.*;
import com.auction.pattern.observer.Observer;
import com.auction.security.AuditLogger;
import com.auction.server.AuctionServer;
import com.auction.server.ClientHandler;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Xử lý tất cả request liên quan đến phiên đấu giá:
 * GET_AUCTIONS, GET_AUCTION_DETAIL, PLACE_BID,
 * CREATE_AUCTION, SET_AUTO_BID, CANCEL_AUTO_BID
 */
public class AuctionHandler {

    public static final ConcurrentHashMap<Integer, Long> lastBidTime = new ConcurrentHashMap<>();
    public static final long BID_THROTTLE_MS = 500;
    private static final AuditLogger audit = AuditLogger.getInstance();

    public static Response handleGetAuctions() {
        return Response.ok(AuctionManager.getInstance().getAuctions());
    }

    public static Response handleGetAuctionDetail(Request request, ClientHandler handler) {
        if (!(request.getPayload() instanceof GetAuctionDetailPayload payload))
            return Response.error("Payload không hợp lệ!");

        Auction auction = AuctionManager.getInstance().findAuctionById(payload.auctionId);
        if (auction == null)
            return Response.error("Không tìm thấy phiên đấu giá " + payload.auctionId);

        if (payload.decrementView) {
            handleDecrementView(auction, handler);
        } else {
            handleIncrementView(auction, handler, payload.incrementView);
        }

        return Response.ok(auction);
    }

    private static void handleDecrementView(Auction auction, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        boolean hasOtherConnection = false;

        if (user != null) {
            for (Observer obs : auction.getObservers()) {
                if (obs != handler && obs instanceof ClientHandler ch) {
                    User other = ch.getLoggedInUser();
                    if (other != null && other.getId() == user.getId()) {
                        hasOtherConnection = true;
                        break;
                    }
                }
            }
        }

        if (!hasOtherConnection) auction.decrementViewCount();
        auction.removeObserver(handler);
        auction.notifyObservers("VIEW_UPDATE");
    }

    private static void handleIncrementView(Auction auction, ClientHandler handler, boolean shouldIncrement) {
        User user = handler.getLoggedInUser();
        boolean alreadyObserving = false;

        if (user != null) {
            for (Observer obs : auction.getObservers()) {
                if (obs instanceof ClientHandler ch) {
                    User other = ch.getLoggedInUser();
                    if (other != null && other.getId() == user.getId()) {
                        alreadyObserving = true;
                        break;
                    }
                }
            }
        }

        if (!auction.hasObserver(handler)) auction.addObserver(handler);

        if (shouldIncrement && !alreadyObserving) {
            auction.incrementViewCount();
            auction.notifyObservers("VIEW_UPDATE");
        }
    }

    public static Response handlePlaceBid(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập để đấu giá!");
        if (!(user instanceof Bidder bidder)) return Response.error("Chỉ Bidder mới có quyền đặt giá!");
        if (!(request.getPayload() instanceof PlaceBidPayload payload))
            return Response.error("Dữ liệu đặt giá không hợp lệ!");

        // Throttle chống spam bid
        long now = System.currentTimeMillis();
        Long last = lastBidTime.get(user.getId());
        if (last != null && now - last < BID_THROTTLE_MS)
            return Response.error("Bạn đang gửi yêu cầu quá nhanh! Vui lòng đợi.");
        lastBidTime.put(user.getId(), now);

        if (payload.amount <= 0) return Response.error("Số tiền đặt giá phải lớn hơn 0!");

        try {
            Auction auction = AuctionManager.getInstance().findAuctionById(payload.auctionId);
            if (auction == null) return Response.error("Phiên đấu giá không tồn tại!");

            auction.placeBid(bidder, payload.amount);
            bidder.getProfile().addParticipatedAuction(payload.auctionId);

            audit.logBid(handler.getClientIp(), user.getId(), payload.auctionId, payload.amount);
            return Response.ok(bidder.getBalance());
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response handleCreateAuction(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể tạo phiên đấu giá");
        if (!(request.getPayload() instanceof CreateAuctionPayload p))
            return Response.error("Payload không hợp lệ!");

        if (p.endTime == null) return Response.error("Thời gian kết thúc không được để trống");
        if (!p.endTime.isAfter(LocalDateTime.now()))
            return Response.error("Thời gian kết thúc phải ở trong tương lai");

        Item item = ItemManager.getInstance().findItem(p.itemId);
        if (item == null) return Response.error("Sản phẩm không tồn tại: " + p.itemId);
        if (item.getOwnerId() != user.getId()) return Response.error("Bạn không phải chủ sở hữu sản phẩm này");

        Auction auction = AuctionManager.getInstance().createAuction(item, p.startTime, p.endTime, p.minBidStep);
        if (auction == null) return Response.error("Không thể tạo phiên — sản phẩm không ở trạng thái AVAILABLE");

        LocalDateTime now = LocalDateTime.now();
        if (item.getStatus() != ItemStatus.PENDING && (p.startTime == null || !p.startTime.isAfter(now))) {
            auction.start();
            DataManager.getInstance().startAuction(auction.getId());
        }

        handler.sendNotification(new Notification("AUCTION_CREATED",
                String.format("Phiên đấu giá cho sản phẩm [%s] đã được tạo thành công!", item.getName())));

        // Thông báo cho tất cả Bidder đang online
        for (ClientHandler client : handler.getServer().getConnectedClients()) {
            if (client.getLoggedInUser() instanceof Bidder) {
                client.sendNotification(new Notification("NEW_AUCTION",
                        String.format("Sản phẩm mới [%s] vừa lên sàn đấu giá! Hãy tham gia ngay.", item.getName())));
            }
        }

        return Response.ok(auction);
    }

    public static Response handleSetAutoBid(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Bidder bidder)) return Response.error("Chỉ Bidder mới có thể dùng Auto-Bid");
        if (!(request.getPayload() instanceof AutoBidPayload p))
            return Response.error("Payload không hợp lệ!");

        Auction auction = AuctionManager.getInstance().findAuctionById(p.auctionId);
        if (auction == null) return Response.error("Phiên không tồn tại");
        if (auction.getStatus() != AuctionStatus.RUNNING)
            return Response.error("Phiên chưa chạy hoặc đã kết thúc");

        if (p.maxBid <= auction.getCurrentHighestBid())
            return Response.error("maxBid phải lớn hơn giá hiện tại ($" + auction.getCurrentHighestBid() + ")");
        if (p.increment < auction.getMinBidStep())
            return Response.error("Bước tăng phải >= minBidStep ($" + auction.getMinBidStep() + ")");

        AutoBidConfig cfg = new AutoBidConfig(bidder.getId(), bidder.getUsername(), p.maxBid, p.increment);
        auction.registerAutoBid(cfg);
        return Response.ok("Đăng ký Auto-Bid thành công (tối đa $" + p.maxBid + ")");
    }

    public static Response handleCancelAutoBid(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(request.getPayload() instanceof Integer auctionId))
            return Response.error("Payload không hợp lệ!");

        Auction auction = AuctionManager.getInstance().findAuctionById(auctionId);
        if (auction != null) auction.cancelAutoBid(user.getId());
        return Response.ok("Đã hủy Auto-Bid");
    }
}