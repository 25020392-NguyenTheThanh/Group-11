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
    public static Response process(Request request, ClientHandler handler) {
        return switch (request.getType()) {
            case LOGIN              -> handleLogin(request, handler);
            case REGISTER           -> handleRegister(request);
            case LOGOUT             -> handleLogout(handler);
            case GET_AUCTIONS       -> handleGetAuctions();
            case GET_AUCTION_DETAIL -> handleGetAuctionDetail(request);
            case PLACE_BID          -> handlePlaceBid(request, handler);
            case CREATE_ITEM        -> handleCreateItem(request, handler);
            case CREATE_AUCTION     -> handleCreateAuction(request, handler);
            case GET_MY_ITEMS       -> handleGetMyItems(handler);
            case DELETE_ITEM        -> handleDeleteItem(request, handler);
            case UPDATE_ITEM        -> handleUpdateItem(request, handler);
        };
    }

    // kiểm tra danh tính
    private static Response handleLogin(Request request, ClientHandler handler) {
        //// kiểm tra payload
        LoginPayload p = (LoginPayload) request.getPayload();
        try {
            User user = UserManager.getInstance().login(p.username, p.password);
            if (user == null) return Response.error("Tên đăng nhập hoặc mật khẩu không đúng!");
            //// lưu User vào handler để biết ai đang gửi request
            handler.setLoggedInUser(user);
            return Response.ok(user);
        } catch (Exception e) {
            return Response.error("Đăng nhập thất bại: " + e.getMessage());
        }
    }

    // đăng kí tài khoản mới
    private static Response handleRegister(Request request) {
        RegisterPayload p = (RegisterPayload) request.getPayload();
        try {
            User user = UserManager.getInstance().register(p.username, p.password, p.email, p.role);
            if (user == null) return Response.error("Đăng ký thành công nhưng không đọc được dữ liệu.");
            return Response.ok(user);
        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage()); // "Username '...' đã tồn tại!" / "Email '...' đã được đăng ký!"
        } catch (Exception e) {
            return Response.error("Lỗi server khi đăng ký: " + e.getMessage());
        }
    }
    // xử lý đăng xuất
    private static Response handleLogout(ClientHandler handler) {
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
    private static Response handlePlaceBid(Request request, ClientHandler handler) {
        PlaceBidPayload payload = (PlaceBidPayload) request.getPayload();
        User user = handler.getLoggedInUser();
        if (user == null)              return Response.error("Bạn cần đăng nhập để đấu giá");
        if (!(user instanceof Bidder)) return Response.error("Chỉ Bidder mới có quyền đặt giá!");
        try {
            Auction auction = AuctionManager.getInstance().findAuctionById(payload.auctionId);
            if (auction == null) return Response.error("Phiên không tồn tại");
            auction.addObserver(handler);
            Bidder bidder = (Bidder) user;
            auction.placeBid(bidder, payload.amount);
            //ghi lich su vao sql
            AuctionManager.getInstance().recordBid(
                    payload.auctionId, bidder.getId(), bidder.getUsername(), payload.amount);
            com.auction.data.DataManager.getInstance().updateBidderBalance(bidder.getId(), bidder.getBalance());
            return Response.ok("Đặt giá thành công");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
    // Lấy chi tiết 1 phiên đấu giá
    private static Response handleGetAuctionDetail(Request request) {
        int auctionId = (Integer) request.getPayload();
        Auction a = AuctionManager.getInstance().findAuctionById(auctionId);
        if (a == null) return Response.error("Không tìm thấy phiên đấu giá " + auctionId);
        return Response.ok(a);
    }
    // Seller tạo sản phẩm mới
    private static Response handleCreateItem(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null)              return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể tạo sản phẩm");
        CreateItemPayload p = (CreateItemPayload) request.getPayload();
        ItemFactory factory = switch (p.type.toUpperCase()) {
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
        com.auction.data.DataManager.getInstance().startAuction(auction.getId());
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

        if (existingItem.getStatus() != com.auction.model.item.ItemStatus.AVAILABLE) {
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
}
