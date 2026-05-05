package com.auction.server;

import com.auction.manager.AuctionManager;
import com.auction.manager.UserManager;
import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.*;

public class RequestProcessor {
    public static Response process(Request request , ClientHandler handler) {
        RequestType type = request.getType();

        if (type == RequestType.LOGIN) {
            return handleLogin(request, handler);
        } else if (type == RequestType.GET_AUCTIONS) {
            return handleGetAuctions();
        } else if (type == RequestType.PLACE_BID) {
            return handlePlaceBid(request, handler);
        } else {
            return Response.error("Yêu cầu không hợp lệ!");
        }
    } // vi phạm OCP

    // kiểm tra danh tính
    private static Response handleLogin(Request request , ClientHandler handler){
        LoginPayload payload = (LoginPayload) request.getPayload();
        try {
            User user = UserManager.getInstance().login(payload.username , payload.password);
            // lưu User vào handler để biết ai đang gửi request
            handler.setLoggedInUser(user);
            return Response.ok(user);
        } catch (Exception e){
            return Response.error("Đăng nhập thất bại : " + e.getMessage());
        }
    }

    // lấy danh sách
    private static Response handleGetAuctions() {
        // Lấy danh sách từ RAM (đã được load từ file .ser lúc khởi động server)
        return Response.ok(AuctionManager.getInstance().getAuctions());
    }

    // xử lý việc trả giá
    private static Response handlePlaceBid(Request request , ClientHandler handler){
        PlaceBidPayload payload = (PlaceBidPayload) request.getPayload();
        User user = handler.getLoggedInUser();
        if (user == null) {
            return Response.error("Bạn cần đăng nhập để đấu giá");
        }
        if (!(user instanceof Bidder)) {
            return Response.error("Chỉ người mua (Bidder) mới có quyền đặt giá!");
        }
        try {
            Auction auction = AuctionManager.getInstance().findAuctionById(payload.auctionId);
            auction.addObserver(handler);
            Bidder bidder = (Bidder) user;
            auction.placeBid(bidder, payload.amount);

            AuctionManager.getInstance().saveToDisk();
            return Response.ok("Đặt giá thành công");
        } catch (Exception e){
            return Response.error(e.getMessage());
        }

    }
}
