package com.auction.server;

import com.auction.data.DataManager;
import com.auction.manager.AuctionManager;
import com.auction.manager.ItemManager;
import com.auction.manager.UserManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.*;
import com.auction.pattern.factory.ArtFactory;
import com.auction.pattern.factory.ElectronicsFactory;
import com.auction.pattern.factory.ItemFactory;
import com.auction.pattern.factory.VehicleFactory;

import java.util.concurrent.ConcurrentHashMap;

// điều phối request từ client gửi lên server
public class RequestProcessor {
    private static final ConcurrentHashMap<Integer, Long> lastBidTime = new ConcurrentHashMap<>();
    // hàm trung tâm
    public static Response process(Request request, ClientHandler handler) {
        try {
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
                case DELETE_ITEM    -> handleDeleteItem(request, handler);
                case UPDATE_ITEM    -> handleUpdateItem(request, handler);
                case ADD_TO_WATCHLIST -> handleAddToWatchlist(request, handler);
                case REMOVE_FROM_WATCHLIST -> handleRemoveFromWatchlist(request, handler);
                case TOP_UP         -> handleTopUp(request, handler);
                case SET_AUTO_BID   -> handleSetAutoBid(request , handler);
                case CANCEL_AUTO_BID -> handleCancelAutoBid(request , handler);
            };
        } catch (Exception e) {
            // Safety net: bắt mọi exception chưa được xử lý trong handler
            // Tránh crash connection khi có lỗi bất ngờ
            System.err.println("[RequestProcessor] Unhandled exception in "
                    + request.getType() + ": " + e.getMessage());
            return Response.error("Lỗi xử lý yêu cầu: " + e.getMessage());
        }
    } // vi phạm OCP

    // kiểm tra danh tính
    private static Response handleLogin(Request request , ClientHandler handler){
        // Kiểm tra payload hợp lệ
        if (request.getPayload() == null || !(request.getPayload() instanceof LoginPayload)) {
            return Response.error("Dữ liệu đăng nhập không hợp lệ!");
        }

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
        if (request.getPayload() == null || !(request.getPayload() instanceof RegisterPayload)) {
            return Response.error("Dữ liệu đăng ký không hợp lệ!");
        }
        RegisterPayload p = (RegisterPayload) request.getPayload();
        try {
            User user = UserManager.getInstance().register(p.username, p.password, p.email, p.role);
            if (user == null) return Response.error("Đăng ký thất bại, vui lòng thử lại.");
            return Response.ok(user);
        } catch (IllegalArgumentException e) {
            // USERNAME_EXISTS, EMAIL_EXISTS hoặc lỗi DB từ UserManager
            return Response.error(e.getMessage());
        } catch (Exception e) {
            System.err.println("[handleRegister] Lỗi không xác định: " + e.getMessage());
            return Response.error("Lỗi hệ thống khi đăng ký: " + e.getMessage());
        }
    }

    // xử lý đăng xuất
    private static Response handleLogout(ClientHandler handler){
        try {
            User user = handler.getLoggedInUser();
            if (user != null) {
                user.logout();
                AuctionManager.getInstance().removeObserverFromAllAuctions(handler);
            }
            handler.setLoggedInUser(null); // Giải phóng user ngắt kết nối logic
            return Response.ok("Đăng xuất thành công");
        } catch (Exception e) {
            return Response.error("Lỗi xóa phiên đăng nhập: " + e.getMessage());
        }
    }
    // lấy danh sách
    private static Response handleGetAuctions() {
        // Lấy danh sách từ RAM (đã được load từ file .ser lúc khởi động server)
        return Response.ok(AuctionManager.getInstance().getAuctions());
    }

    // xử lý việc trả giá
    private static Response handlePlaceBid(Request request, ClientHandler handler) {
        PlaceBidPayload payload = (PlaceBidPayload) request.getPayload();
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập để đấu giá");
        if (!(user instanceof Bidder)) return Response.error("Chỉ người mua (Bidder) mới có quyền đặt giá!");

        try {
            Auction auction = AuctionManager.getInstance().findAuctionById(payload.auctionId);
            if (auction == null) return Response.error("Phiên không tồn tại");
            Bidder bidder = (Bidder) user;
            auction.placeBid(bidder, payload.amount);
            bidder.getProfile().addParticipatedAuction(payload.auctionId);

            AuctionManager.getInstance().recordBid(
                    payload.auctionId,
                    bidder.getId(),
                    bidder.getUsername(),
                    payload.amount
            );

            // Broadcast BID_UPDATE tới TẤT CẢ client (kể cả đang ở màn hình danh sách)
            BidUpdateData upd = new BidUpdateData(
                    payload.auctionId,
                    auction.getCurrentHighestBid(),
                    bidder.getUsername(),
                    auction.getBidHistory().size()
            );
            handler.getServer().broadcast(new Notification("BID_UPDATE", upd));

            return Response.ok("Đặt giá thành công");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
    // Lấy chi tiết 1 phiên đấu giá và xử lý tăng/giảm lượt xem hoặc đăng ký/hủy observer
    private static Response handleGetAuctionDetail(Request request , ClientHandler handler){
        GetAuctionDetailPayload payload = (GetAuctionDetailPayload) request.getPayload();
        int auctionId = payload.auctionId;
        Auction a = AuctionManager.getInstance().findAuctionById(auctionId);
        if (a == null ){
            return Response.error("Không tìm thấy phiên đấu giá " + auctionId);
        }

        if (payload.decrementView) {
            a.decrementViewCount();
            a.removeObserver(handler);
            a.notifyObservers("VIEW_UPDATE");
        } else {
            // Đăng ký client handler làm observer của phiên đấu giá để nhận thông báo realtime
            if (!a.hasObserver(handler)) {
                a.addObserver(handler);
            }
            // Tăng lượt xem và thông báo realtime cho các client khác nếu click CHI TIẾT
            if (payload.incrementView) {
                a.incrementViewCount();
                a.notifyObservers("VIEW_UPDATE");
            }
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
        };
        Item item = ItemManager.getInstance().createItem(factory, user.getId(), p.name, p.description, p.startingPrice, p.imageUrl);
        if (item == null) return Response.error("Không thể tạo sản phẩm, vui lòng thử lại.");
        return Response.ok(item);
    }
    // Seller tạo phiên đấu giá từ item
    private static Response handleCreateAuction(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null)              return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể tạo phiên đấu giá");
        CreateAuctionPayload p = (CreateAuctionPayload) request.getPayload();

        // Validate endTime: không được null và phải ở tương lai
        if (p.endTime == null) return Response.error("Thời gian kết thúc không được để trống");
        if (!p.endTime.isAfter(java.time.LocalDateTime.now()))
            return Response.error("Thời gian kết thúc phải ở trong tương lai");

        Item item = ItemManager.getInstance().findItem(p.itemId);
        if (item == null)                      return Response.error("Sản phẩm không tồn tại: " + p.itemId);
        if (item.getOwnerId() != user.getId()) return Response.error("Bạn không phải chủ sở hữu sản phẩm này");
        Auction auction = AuctionManager.getInstance().createAuction(item, p.startTime, p.endTime, p.minBidStep);
        if (auction == null) return Response.error("Không thể tạo phiên — sản phẩm không ở trạng thái AVAILABLE");

        // Đúng thứ tự: start() đổi status RAM trước, rồi mới sync xuống DB
        auction.start();
        DataManager.getInstance().startAuction(auction.getId());
        return Response.ok(auction);
    }
    // lấy danh sách item của user hiện tại
    private static Response handleGetMyItems(ClientHandler handler){
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        return Response.ok(ItemManager.getInstance().getByOwner(user.getId()));
    }

    // Xóa sản phẩm
    private static Response handleDeleteItem(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể xóa sản phẩm");

        if (request.getPayload() == null || !(request.getPayload() instanceof Integer)) {
            return Response.error("Dữ liệu xóa sản phẩm không hợp lệ!");
        }

        int itemId = (Integer) request.getPayload();
        Item item = ItemManager.getInstance().findItem(itemId);
        if (item == null) {
            return Response.error("Sản phẩm không tồn tại");
        }

        if (item.getOwnerId() != user.getId()) {
            return Response.error("Bạn không phải chủ sở hữu của sản phẩm này");
        }

        if (item.getStatus() != com.auction.model.item.ItemStatus.AVAILABLE) {
            return Response.error("Chỉ có thể xóa sản phẩm ở trạng thái AVAILABLE");
        }

        boolean success = ItemManager.getInstance().deleteItem(itemId);
        if (success) {
            return Response.ok("Xóa sản phẩm thành công");
        } else {
            return Response.error("Không thể xóa sản phẩm khỏi cơ sở dữ liệu");
        }
    }

    // Sửa sản phẩm
    private static Response handleUpdateItem(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể sửa sản phẩm");

        if (request.getPayload() == null || !(request.getPayload() instanceof UpdateItemPayload)) {
            return Response.error("Dữ liệu sửa sản phẩm không hợp lệ!");
        }

        UpdateItemPayload p = (UpdateItemPayload) request.getPayload();
        Item existingItem = ItemManager.getInstance().findItem(p.id);
        if (existingItem == null) {
            return Response.error("Sản phẩm không tồn tại");
        }

        if (existingItem.getOwnerId() != user.getId()) {
            return Response.error("Bạn không phải chủ sở hữu của sản phẩm này");
        }

        if (existingItem.getStatus() != ItemStatus.AVAILABLE) {
            return Response.error("Chỉ có thể sửa sản phẩm ở trạng thái AVAILABLE");
        }

        ItemFactory factory = switch (p.type.toUpperCase()) {
            case "ART"     -> new ArtFactory(p.artist);
            case "VEHICLE" -> new VehicleFactory(p.year);
            default        -> new ElectronicsFactory(p.brand);
        };

        // Tạo item mới dựa trên factory để thay thế
        Item updatedItem = factory.createItem(p.id, user.getId(), p.name, p.description, p.startingPrice, p.imageUrl);

        boolean success = ItemManager.getInstance().updateItem(updatedItem);
        if (success) {
            return Response.ok("Cập nhật sản phẩm thành công");
        } else {
            return Response.error("Không thể cập nhật sản phẩm trong cơ sở dữ liệu");
        }
    }

    private static Response handleAddToWatchlist(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập");
        if (!(user instanceof Bidder)) return Response.error("Chỉ Bidder mới có thể theo dõi");

        int auctionId = (Integer) request.getPayload();
        Bidder bidder = (Bidder) user;

        boolean success = DataManager.getInstance().addToWatchlist(bidder.getId(), auctionId);
        if (success) {
            bidder.getProfile().addToWatchlist(auctionId);
            return Response.ok("Đã thêm vào danh sách theo dõi");
        } else {
            return Response.error("Không thể thêm vào danh sách theo dõi");
        }
    }

    private static Response handleRemoveFromWatchlist(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập");
        if (!(user instanceof Bidder)) return Response.error("Chỉ Bidder mới có thể hủy theo dõi");

        int auctionId = (Integer) request.getPayload();
        Bidder bidder = (Bidder) user;

        boolean success = DataManager.getInstance().removeFromWatchlist(bidder.getId(), auctionId);
        if (success) {
            bidder.getProfile().removeFromWatchlist(auctionId);
            return Response.ok("Đã xóa khỏi danh sách theo dõi");
        } else {
            return Response.error("Không thể xóa khỏi danh sách theo dõi");
        }
    }

    private static Response handleTopUp(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) {
            return Response.error("Bạn cần đăng nhập để nạp tiền");
        }
        if (!(user instanceof Bidder)) {
            return Response.error("Chỉ người mua (Bidder) mới có quyền nạp tiền!");
        }
        if (request.getPayload() == null || !(request.getPayload() instanceof Double)) {
            return Response.error("Dữ liệu nạp tiền không hợp lệ!");
        }

        double amount = (Double) request.getPayload();
        if (amount <= 0) {
            return Response.error("Số tiền nạp phải lớn hơn 0");
        }

        try {
            Bidder bidder = (Bidder) user;
            double newBalance = bidder.getBalance() + amount;

            // Đồng bộ trực tiếp vào cơ sở dữ liệu
            boolean success = DataManager.getInstance().updateBidderBalance(bidder.getId(), newBalance);
            if (success) {
                bidder.topUp(amount);
                return Response.ok(bidder.getBalance());
            } else {
                return Response.error("Không thể cập nhật số dư vào cơ sở dữ liệu");
            }
        } catch (Exception e) {
            return Response.error("Lỗi nạp tiền: " + e.getMessage());
        }
    }

    private static Response handleSetAutoBid(Request request , ClientHandler handler){
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Bidder)) return Response.error("Chỉ Bidder mới có thể dùng Auto-Bid ");

        AutoBidPayload p = (AutoBidPayload) request.getPayload();
        Auction auction = AuctionManager.getInstance().findAuctionById(p.auctionId);
        if (auction == null) return Response.error("Phiên không tồn tại");
        if (auction.getStatus() != com.auction.model.auction.AuctionStatus.RUNNING)
            return Response.error("Phiên chưa chạy hoặc đã kết thúc");

        Bidder bidder = (Bidder) user;
        if (p.maxBid <= auction.getCurrentHighestBid())
            return Response.error("maxBid phải lớn hơn giá hiện tại (" + auction.getCurrentHighestBid() + "₫)");
        if (p.increment < auction.getMinBidStep())
            return Response.error("Bước tăng phải >= minBidStep (" + auction.getMinBidStep() + "₫)");
        AutoBidConfig cfg = new AutoBidConfig(bidder.getId(), bidder.getUsername(), p.maxBid, p.increment);
        auction.registerAutoBid(cfg);
        return Response.ok("Đăng ký Auto-Bid thành công (tối đa " + p.maxBid + "₫)");
    }

    private static Response handleCancelAutoBid(Request request , ClientHandler handler){
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        int auctionId = (Integer) request.getPayload();
        Auction auction = AuctionManager.getInstance().findAuctionById(auctionId);
        if (auction != null) auction.cancelAutoBid(user.getId());
        return Response.ok("Đã hủy Auto_Bid");
    }
}