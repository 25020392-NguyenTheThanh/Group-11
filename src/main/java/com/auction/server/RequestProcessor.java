package com.auction.server;

import com.auction.manager.AuctionManager;
import com.auction.manager.ItemManager;
import com.auction.manager.UserManager;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.*;
import com.auction.pattern.factory.ArtFactory;
import com.auction.pattern.factory.ElectronicsFactory;
import com.auction.pattern.factory.ItemFactory;
import com.auction.pattern.factory.VehicleFactory;

// điều phối request từ client gửi lên server
public class RequestProcessor {
    // hàm trung tâm
    public static Response process(Request request , ClientHandler handler) {
        return switch (request.getType()) {
            case LOGIN          -> handleLogin(request, handler);
            case REGISTER       -> handleRegister(request);
            case LOGOUT         -> handleLogout(handler);
            case GET_AUCTIONS   -> handleGetAuctions();
            case GET_AUCTION_DETAIL -> handleGetAuctionDetail(request, handler);
            case PLACE_BID      -> handlePlaceBid(request, handler);
            case CREATE_ITEM    -> handleCreateItem(request, handler);
            case CREATE_AUCTION -> handleCreateAuction(request, handler);
            case GET_MY_ITEMS   -> handleGetMyItems(handler);
        };
    } // vi phạm OCP

    // kiểm tra danh tính
    private static Response handleLogin(Request request , ClientHandler handler){
        LoginPayload payload = (LoginPayload) request.getPayload();
        try {
            User user = UserManager.getInstance().login(payload.username , payload.password);
            if (user == null) {
                return Response.error("Tên đăng nhập hoặc mật khẩu không đúng!");
            }
            // lưu User vào handler để biết ai đang gửi request
            handler.setLoggedInUser(user);
            return Response.ok(user);
        } catch (Exception e){
            return Response.error("Đăng nhập thất bại: " + e.getMessage());
        }
    }

    // đăng kí tài khoản mới
    private static Response handleRegister(Request request){
        RegisterPayload p = (RegisterPayload) request.getPayload();
        User user = UserManager.getInstance().register(p.username , p.password , p.email , p.role);
        if (user == null) return Response.error("Username đã tồn tại");
        return Response.ok(user);
    }

    // xử lý đăng xuất
    private static Response handleLogout(ClientHandler handler){
        User user = handler.getLoggedInUser();
        if (user != null) user.logout();
        handler.setLoggedInUser(null);
        return Response.ok("Đăng xuất thành công");
    }
    // lấy danh sách
    private static Response handleGetAuctions() {
        // Lấy danh sách từ RAM (đã được load từ file .ser lúc khởi động server)
        return Response.ok(AuctionManager.getInstance().getAuctions());
    }

    // xử lý việc trả giá
    private static Response handlePlaceBid(Request request , ClientHandler handler) {
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
            if (auction == null) return Response.error("Phiên không tồn tại");
            Bidder bidder = (Bidder) user;
            auction.placeBid(bidder, payload.amount); // synchronized , throws nếu lỗi

            // Ghi lịch sử bid vào MySQL
            AuctionManager.getInstance().recordBid(
                    payload.auctionId,
                    bidder.getId(),
                    bidder.getUsername(),
                    payload.amount
            );

            BidUpdateData updateData = new BidUpdateData(
                    payload.auctionId,
                    auction.getCurrentHighestBid(),
                    bidder.getUsername(),
                    auction.getBidHistory().size()
            );
            handler.getServer().broadcast(new Notification("BID_UPDATE" , updateData));
            return Response.ok("Đặt giá thành công");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
    // Lấy chi tiết 1 phiên đấu giá
    private static Response handleGetAuctionDetail(Request request , ClientHandler handler){
        int auctionId = (Integer) request.getPayload();
        Auction a = AuctionManager.getInstance().findAuctionById(auctionId);
        if (a == null ){
            return Response.error("Không tìm thấy phiên đấu giá " + auctionId);
        }
        if (!a.hasObserver(handler)) {
            a.addObserver(handler);
        }
        return Response.ok(a);
    }
    // Seller tạo sản phẩm mới
    private static Response handleCreateItem(Request request , ClientHandler handler){
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể tạo sản phẩm");

        CreateItemPayload p = (CreateItemPayload) request.getPayload();
        ItemFactory factory = switch (p.type.toUpperCase()){
            case "ART"     -> new ArtFactory(p.artist);
            case "VEHICLE" -> new VehicleFactory(p.year);
            default        -> new ElectronicsFactory(p.brand);
        }; // code smell , ở createItem
        Item item = ItemManager.getInstance().createItem(factory , user.getId() , p.name , p.description , p.startingPrice);
        return Response.ok(item);
    }
    // Seller tạo phiên đấu giá từ item
    private static Response handleCreateAuction(Request request , ClientHandler handler){
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể phiên đấu giá");

        CreateAuctionPayload p = (CreateAuctionPayload) request.getPayload();
        Item item = ItemManager.getInstance().findItem(p.itemId);
        if (item  == null) return Response.error("Sản phẩm không tồn tại : " + p.itemId);
        if (item.getOwnerId() != user.getId()) return Response.error("Bạn không phải chủ sở hữu của sản phẩm này");

        Auction auction = AuctionManager.getInstance().createAuction(item , p.endTime , p.minBidStep);
        if (auction == null) return Response.error("Không thể tạo phiên - sản phẩm không ở trạng thái AVAILABLE");

        auction.start(); // chuyển sang RUNNING
        // Đồng bộ trạng thái RUNNING vào DB
        com.auction.data.DataManager.getInstance().startAuction(auction.getId());
        return Response.ok(auction);
    }
    // lấy danh sách item của user hiện tại
    private static Response handleGetMyItems(ClientHandler handler){
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        return Response.ok(ItemManager.getInstance().getByOwner(user.getId()));
    }

}
