package com.auction.server.handler;

import com.auction.data.DataManager;
import com.auction.manager.AuctionManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.Notification;
import com.auction.network.Request;
import com.auction.network.Response;
import com.auction.server.AuctionServer;
import com.auction.server.ClientHandler;

import java.time.LocalDateTime;

/**
 * Xử lý tất cả request liên quan đến Bidder:
 * ADD_TO_WATCHLIST, REMOVE_FROM_WATCHLIST, TOP_UP,
 * CONFIRM_PAYMENT, DECLINE_PAYMENT
 */
public class BidderHandler {

    public static Response handleAddToWatchlist(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập");
        if (!(user instanceof Bidder bidder)) return Response.error("Chỉ Bidder mới có thể theo dõi");
        if (!(request.getPayload() instanceof Integer auctionId)) return Response.error("Payload không hợp lệ!");

        boolean ok = DataManager.getInstance().addToWatchlist(bidder.getId(), auctionId);
        if (ok) {
            bidder.getProfile().addToWatchlist(auctionId);
            return Response.ok("Đã thêm vào danh sách theo dõi");
        }
        return Response.error("Không thể thêm vào danh sách theo dõi");
    }

    public static Response handleRemoveFromWatchlist(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập");
        if (!(user instanceof Bidder bidder)) return Response.error("Chỉ Bidder mới có thể hủy theo dõi");
        if (!(request.getPayload() instanceof Integer auctionId)) return Response.error("Payload không hợp lệ!");

        boolean ok = DataManager.getInstance().removeFromWatchlist(bidder.getId(), auctionId);
        if (ok) {
            bidder.getProfile().removeFromWatchlist(auctionId);
            return Response.ok("Đã xóa khỏi danh sách theo dõi");
        }
        return Response.error("Không thể xóa khỏi danh sách theo dõi");
    }

    public static Response handleTopUp(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập để nạp tiền");
        if (!(user instanceof Bidder bidder)) return Response.error("Chỉ người mua (Bidder) mới có quyền nạp tiền!");
        if (!(request.getPayload() instanceof Double amount)) return Response.error("Dữ liệu nạp tiền không hợp lệ!");
        if (amount <= 0) return Response.error("Số tiền nạp phải lớn hơn 0");

        try {
            boolean ok = DataManager.getInstance().addBidderBalance(bidder.getId(), amount);
            if (!ok) return Response.error("Không thể cập nhật số dư vào cơ sở dữ liệu");

            DataManager.getInstance().markBidderToppedUp(bidder.getId());
            double newBalance = DataManager.getInstance().getBidderBalance(bidder.getId());
            bidder.setBalance(newBalance);
            bidder.setLastTopUpTime(LocalDateTime.now());
            return Response.ok(bidder.getBalance());
        } catch (Exception e) {
            return Response.error("Lỗi nạp tiền: " + e.getMessage());
        }
    }

    public static Response handleConfirmPayment(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập để thanh toán!");
        if (!(user instanceof Bidder bidder)) return Response.error("Chỉ người mua (Bidder) mới có thể thực hiện thanh toán!");
        if (!(request.getPayload() instanceof Integer auctionId)) return Response.error("Dữ liệu thanh toán không hợp lệ!");

        Auction auction = AuctionManager.getInstance().findAuctionById(auctionId);
        if (auction == null) return Response.error("Phiên đấu giá không tồn tại!");
        if (auction.getStatus() != AuctionStatus.FINISHED) return Response.error("Phiên đấu giá chưa kết thúc hoặc đã được thanh toán!");
        if (auction.getCurrentWinner() == null || auction.getCurrentWinner().getId() != user.getId())
            return Response.error("Bạn không phải người thắng cuộc của phiên này!");

        try {
            auction.markPaid();

            boolean dbOk = AuctionManager.getInstance().payAuction(auctionId);
            if (!dbOk) {
                auction.restoreStatus(AuctionStatus.FINISHED);
                return Response.error("Không thể cập nhật trạng thái thanh toán vào cơ sở dữ liệu!");
            }

            // Ghi nhận lịch sử thắng
            DataManager.getInstance().saveBidderWon(bidder.getId(), auctionId);
            if (bidder.getProfile() != null) bidder.getProfile().addWonAuction(auctionId);

            // Cộng doanh thu Seller
            int sellerId = auction.getItem().getOwnerId();
            DataManager.getInstance().updateSellerRevenue(sellerId, auction.getCurrentHighestBid());

            // Thông báo cho Bidder
            handler.sendNotification(new Notification("PAYMENT_SUCCESS", String.format(
                    "Bạn đã thanh toán thành công $%,.2f cho sản phẩm [%s] — Phiên #%d.",
                    auction.getCurrentHighestBid(), auction.getItem().getName(), auctionId)));

            // Thông báo cho Seller (nếu online)
            notifySeller(sellerId, String.format(
                            "💰 [%s] đã thanh toán $%,.2f cho sản phẩm [%s] — Phiên #%d.",
                            bidder.getUsername(), auction.getCurrentHighestBid(), auction.getItem().getName(), auctionId),
                    "PAYMENT_RECEIVED", handler);

            // Broadcast để tất cả client reload
            broadcastAuctionEnded(auctionId, auction.getItem().getName(), handler);

            return Response.ok("Thanh toán thành công!");

        } catch (IllegalStateException e) {
            return Response.error("Lỗi trạng thái: " + e.getMessage());
        } catch (Exception e) {
            return Response.error("Lỗi xử lý thanh toán: " + e.getMessage());
        }
    }

    public static Response handleDeclinePayment(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập để từ chối thanh toán!");
        if (!(user instanceof Bidder)) return Response.error("Chỉ người mua (Bidder) mới có thể từ chối thanh toán!");
        if (!(request.getPayload() instanceof Integer auctionId)) return Response.error("Dữ liệu không hợp lệ!");

        Auction auction = AuctionManager.getInstance().findAuctionById(auctionId);
        if (auction == null) return Response.error("Phiên đấu giá không tồn tại!");
        if (auction.getStatus() != AuctionStatus.FINISHED)
            return Response.error("Chỉ có thể từ chối thanh toán khi phiên ở trạng thái FINISHED!");
        if (auction.getCurrentWinner() == null || auction.getCurrentWinner().getId() != user.getId())
            return Response.error("Bạn không phải người thắng cuộc của phiên này!");

        try {
            boolean ok = AuctionManager.getInstance().cancelAuctionByAdmin(auctionId, "Người mua từ chối thanh toán.");
            if (!ok) return Response.error("Không thể xử lý từ chối thanh toán.");

            int sellerId = auction.getItem().getOwnerId();
            notifySeller(sellerId, String.format(
                            "❌ Người thắng [%s] đã từ chối thanh toán cho sản phẩm [%s] — Phiên #%d. Phiên đấu giá bị hủy.",
                            user.getUsername(), auction.getItem().getName(), auctionId),
                    "PAYMENT_DECLINED", handler);

            return Response.ok("Đã từ chối thanh toán và hủy phiên thành công!");
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }

    // Gửi notification đến Seller nếu đang online
    private static void notifySeller(int sellerId, String message, String type, ClientHandler handler) {
        if (AuctionServer.getInstance() == null) return;
        AuctionServer.getInstance().getConnectedClients().stream()
                .filter(c -> c.getLoggedInUser() != null && c.getLoggedInUser().getId() == sellerId)
                .findFirst()
                .ifPresent(c -> c.sendNotification(new Notification(type, message)));
    }

    private static void broadcastAuctionEnded(int auctionId, String itemName, ClientHandler handler) {
        if (AuctionServer.getInstance() == null) return;
        AuctionServer.getInstance().broadcast(new Notification("AUCTION_ENDED",
                String.format("Phiên #%d [%s] đã được thanh toán.", auctionId, itemName)));
    }
}