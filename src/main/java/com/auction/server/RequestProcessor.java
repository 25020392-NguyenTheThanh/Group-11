package com.auction.server;

import com.auction.client.ServerConnection;
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
import com.auction.pattern.observer.Observer;
import com.example.group11.controller.NotificationController;
import javafx.concurrent.Task;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

// điều phối request từ client gửi lên server
public class RequestProcessor {
    private static final ConcurrentHashMap<Integer, Long> lastBidTime = new ConcurrentHashMap<>();

    // hàm trung tâm
    public static Response process(Request request, ClientHandler handler) {
        try {
            return switch (request.getType()) {
                case LOGIN -> handleLogin(request, handler);
                case REGISTER -> handleRegister(request);
                case LOGOUT -> handleLogout(handler);
                case GET_AUCTIONS -> handleGetAuctions();
                case GET_AUCTION_DETAIL -> handleGetAuctionDetail(request, handler);
                case PLACE_BID -> handlePlaceBid(request, handler);
                case CREATE_ITEM -> handleCreateItem(request, handler);
                case CREATE_AUCTION -> handleCreateAuction(request, handler);
                case GET_MY_ITEMS -> handleGetMyItems(handler);
                case DELETE_ITEM -> handleDeleteItem(request, handler);
                case UPDATE_ITEM -> handleUpdateItem(request, handler);
                case ADD_TO_WATCHLIST -> handleAddToWatchlist(request, handler);
                case REMOVE_FROM_WATCHLIST -> handleRemoveFromWatchlist(request, handler);
                case TOP_UP -> handleTopUp(request, handler);
                case CONFIRM_PAYMENT -> handleConfirmPayment(request, handler);
                case DECLINE_PAYMENT -> handleDeclinePayment(request, handler);
                case SET_AUTO_BID -> handleSetAutoBid(request, handler);
                case CANCEL_AUTO_BID -> handleCancelAutoBid(request, handler);
                case CHANGE_PASSWORD -> handleChangePassword(request, handler);
                case VERIFY_EMAIL -> handleVerifyEmail(request);
                case RESET_PASSWORD -> handleResetPassword(request);

                // --- ADMIN CASE ---
                case ADMIN_GET_ALL_USERS -> handleAdminGetAllUsers(handler);
                case ADMIN_BAN_USER -> handleAdminBanUser(request, handler);
                case ADMIN_UNBAN_USER -> handleAdminUnbanUser(request, handler);
                case ADMIN_DELETE_USER -> handleAdminDeleteUser(request, handler);
                case ADMIN_RESET_USER_PASSWORD -> handleAdminResetUserPassword(request, handler);
                case ADMIN_GET_ALL_AUCTIONS -> handleAdminGetAllAuctions(handler);
                case ADMIN_CANCEL_AUCTION -> handleAdminCancelAuction(request, handler);
                case ADMIN_GET_ALL_ITEMS -> handleAdminGetAllItems(handler);
                case ADMIN_FORCE_DELETE_ITEM -> handleAdminForceDeleteItem(request, handler);
                case ADMIN_GET_STATS -> handleAdminGetStats(handler);
                case ADMIN_GET_AUDIT_LOG -> handleAdminGetAuditLog(handler);
                case ADMIN_GET_ACTIVE_SESSIONS -> handleAdminGetActiveSessions(handler);
                case ADMIN_KICK_USER -> handleAdminKickUser(request, handler);
            };
        } catch (Exception e) {
            // Safety net: bắt mọi exception chưa được xử lý trong handler
            // Tránh crash connection khi có lỗi bất ngờ
            System.err.println("[RequestProcessor] Unhandled exception in "
                    + request.getType() + ": " + e.getMessage());
            return Response.error("Lỗi xử lý yêu cầu: " + e.getMessage());
        }
    }

    // Kiểm tra xem User hiện tại có phải là Admin hay không
    private static boolean isAdmin(User user) {
        if (user == null) return false;
        // Bạn có thể check qua user.getRole().equals("ADMIN") hoặc tùy cấu trúc thuộc tính Role của bạn
        return "ADMIN".equalsIgnoreCase(user.getRole());
    }

    // kiểm tra danh tính
    private static Response handleLogin(Request request, ClientHandler handler) {
        // Kiểm tra payload hợp lệ
        if (request.getPayload() == null || !(request.getPayload() instanceof LoginPayload)) {
            return Response.error("Dữ liệu đăng nhập không hợp lệ!");
        }

        LoginPayload payload = (LoginPayload) request.getPayload();
        try {
            User user = UserManager.getInstance().login(payload.username, payload.password);
            if (user == null) {
                return Response.error("Tên đăng nhập hoặc mật khẩu không đúng!");
            }
            if (!user.isActive()) {
                return Response.error("Tài khoản của bạn đã bị khóa! Lý do: " + user.getBanReason());
            }
            // lưu User vào handler để biết ai đang gửi request
            handler.setLoggedInUser(user);
            return Response.ok(user);
        } catch (Exception e) {
            return Response.error("Đăng nhập thất bại: " + e.getMessage());
        }
    }

    // đăng kí tài khoản mới
    private static Response handleRegister(Request request) {
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
    private static Response handleLogout(ClientHandler handler) {
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

            // Broadcast BID_UPDATE tới TẤT CẢ client (kể cả đang ở màn hình danh sách)
            BidUpdateData upd = new BidUpdateData(
                    payload.auctionId,
                    auction.getCurrentHighestBid(),
                    bidder.getUsername(),
                    auction.getBidHistory().size()
            );
            handler.getServer().broadcast(new Notification("BID_UPDATE", upd));

            return Response.ok(bidder.getBalance());
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    // Lấy chi tiết 1 phiên đấu giá và xử lý tăng/giảm lượt xem hoặc đăng ký/hủy observer
    private static Response handleGetAuctionDetail(Request request, ClientHandler handler) {
        GetAuctionDetailPayload payload = (GetAuctionDetailPayload) request.getPayload();
        int auctionId = payload.auctionId;
        Auction a = AuctionManager.getInstance().findAuctionById(auctionId);
        if (a == null) {
            return Response.error("Không tìm thấy phiên đấu giá " + auctionId);
        }

        if (payload.decrementView) {
            User u = handler.getLoggedInUser();
            boolean hasOtherConnection = false;
            if (u != null) {
                for (com.auction.pattern.observer.Observer obs : a.getObservers()) {
                    if (obs != handler && obs instanceof ClientHandler ch) {
                        User otherUser = ch.getLoggedInUser();
                        if (otherUser != null && otherUser.getId() == u.getId()) {
                            hasOtherConnection = true;
                            break;
                        }
                    }
                }
            }
            if (!hasOtherConnection) {
                a.decrementViewCount();
            }
            a.removeObserver(handler);
            a.notifyObservers("VIEW_UPDATE");
        } else {
            User u = handler.getLoggedInUser();
            boolean userAlreadyObserving = false;
            if (u != null) {
                for (Observer obs : a.getObservers()) {
                    if (obs instanceof ClientHandler ch) {
                        User otherUser = ch.getLoggedInUser();
                        if (otherUser != null && otherUser.getId() == u.getId()) {
                            userAlreadyObserving = true;
                            break;
                        }
                    }
                }
            }
            // Đăng ký client handler làm observer của phiên đấu giá để nhận thông báo realtime
            if (!a.hasObserver(handler)) {
                a.addObserver(handler);
            }
            // Tăng lượt xem và thông báo realtime cho các client khác nếu click CHI TIẾT
            if (payload.incrementView && !userAlreadyObserving) {
                a.incrementViewCount();
                a.notifyObservers("VIEW_UPDATE");
            }
        }
        return Response.ok(a);
    }

    // Seller tạo sản phẩm mới
    private static Response handleCreateItem(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể tạo sản phẩm");

        CreateItemPayload p = (CreateItemPayload) request.getPayload();
        if (p.startingPrice < 100) {
            return Response.error("Giá khởi điểm tối thiểu là 100!");
        }
        ItemFactory factory = switch (p.type.toUpperCase()) {
            case "ART" -> new ArtFactory(p.artist);
            case "VEHICLE" -> new VehicleFactory(p.year);
            default -> new ElectronicsFactory(p.brand);
        };
        Item item = ItemManager.getInstance().createItem(factory, user.getId(), p.name, p.description, p.startingPrice, p.imageUrl);
        if (item == null) return Response.error("Không thể tạo sản phẩm, vui lòng thử lại.");
        handler.sendNotification(new Notification("PRODUCT_APPROVED", String.format("Sản phẩm [%s] của bạn đã được phê duyệt và tạo thành công!", item.getName())));
        return Response.ok(item);
    }

    // Seller tạo phiên đấu giá từ item
    private static Response handleCreateAuction(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể tạo phiên đấu giá");
        CreateAuctionPayload p = (CreateAuctionPayload) request.getPayload();

        // Validate endTime: không được null và phải ở tương lai
        if (p.endTime == null) return Response.error("Thời gian kết thúc không được để trống");
        if (!p.endTime.isAfter(java.time.LocalDateTime.now()))
            return Response.error("Thời gian kết thúc phải ở trong tương lai");

        Item item = ItemManager.getInstance().findItem(p.itemId);
        if (item == null) return Response.error("Sản phẩm không tồn tại: " + p.itemId);
        if (item.getOwnerId() != user.getId()) return Response.error("Bạn không phải chủ sở hữu sản phẩm này");
        Auction auction = AuctionManager.getInstance().createAuction(item, p.startTime, p.endTime, p.minBidStep);
        if (auction == null) return Response.error("Không thể tạo phiên — sản phẩm không ở trạng thái AVAILABLE");

        // Chỉ kích hoạt (start) phiên đấu giá nếu thời gian bắt đầu đã đến hoặc ở quá khứ
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (p.startTime == null || !p.startTime.isAfter(now)) {
            auction.start();
            com.auction.data.DataManager.getInstance().startAuction(auction.getId());
        }
        handler.sendNotification(new Notification("AUCTION_CREATED", String.format("Phiên đấu giá cho sản phẩm [%s] đã được tạo thành công!", item.getName())));

        // Gửi thông báo đấu giá mới cho tất cả các client là Bidder
        for (ClientHandler client : handler.getServer().getConnectedClients()) {
            User u = client.getLoggedInUser();
            if (u instanceof Bidder) {
                client.sendNotification(new Notification("NEW_AUCTION", String.format("Sản phẩm mới [%s] vừa lên sàn đấu giá! Hãy tham gia ngay.", item.getName())));
            }
        }
        return Response.ok(auction);
    }

    // lấy danh sách item của user hiện tại
    private static Response handleGetMyItems(ClientHandler handler) {
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

        if (item.getStatus() == com.auction.model.item.ItemStatus.IN_AUCTION) {
            return Response.error("Không thể xóa sản phẩm đang ở trạng thái IN_AUCTION");
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
        if (p.startingPrice < 100) {
            return Response.error("Giá khởi điểm phải lớn hơn hoặc bằng 100!");
        }
        Item existingItem = ItemManager.getInstance().findItem(p.id);
        if (existingItem == null) {
            return Response.error("Sản phẩm không tồn tại");
        }

        if (existingItem.getOwnerId() != user.getId()) {
            return Response.error("Bạn không phải chủ sở hữu của sản phẩm này");
        }

        if (existingItem.getStatus() == ItemStatus.IN_AUCTION) {
            com.auction.model.auction.Auction auction = com.auction.manager.AuctionManager.getInstance().getAuctions().stream()
                    .filter(a -> a.getItem().getId() == existingItem.getId())
                    .findFirst().orElse(null);
            if (auction != null && auction.getStatus() != com.auction.model.auction.AuctionStatus.OPEN) {
                return Response.error("Chỉ có thể sửa sản phẩm khi phiên đấu giá chưa bắt đầu (OPEN).");
            }
        } else if (existingItem.getStatus() != ItemStatus.AVAILABLE) {
            return Response.error("Chỉ có thể sửa sản phẩm ở trạng thái AVAILABLE hoặc phiên chưa bắt đầu.");
        }

        ItemFactory factory = switch (p.type.toUpperCase()) {
            case "ART" -> new ArtFactory(p.artist);
            case "VEHICLE" -> new VehicleFactory(p.year);
            default -> new ElectronicsFactory(p.brand);
        };

        // Tạo item mới dựa trên factory để thay thế
        Item updatedItem = factory.createItem(p.id, user.getId(), p.name, p.description, p.startingPrice, p.imageUrl);
        updatedItem.setStatus(existingItem.getStatus());

        boolean success = ItemManager.getInstance().updateItem(updatedItem);
        if (success) {
            com.auction.model.auction.Auction auction = com.auction.manager.AuctionManager.getInstance().getAuctions().stream()
                    .filter(a -> a.getItem().getId() == existingItem.getId())
                    .findFirst().orElse(null);
            if (auction != null) {
                auction.setItem(updatedItem);
            }
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
            if (!bidder.canTopUp()) {
                if (bidder.getLastTopUpTime() != null) {
                    java.time.Duration diff = java.time.Duration.between(java.time.LocalDateTime.now(), bidder.getLastTopUpTime().plusHours(24));
                    long hours = diff.toHours();
                    long mins = diff.toMinutesPart();
                    return Response.error(String.format("Bạn chỉ được nạp tiền 1 lần mỗi 24 giờ. Vui lòng thử lại sau %d giờ %d phút.", hours, mins));
                }
                return Response.error("Bạn chỉ được nạp tiền 1 lần mỗi 24 giờ.");
            }

            // Cộng dồn trực tiếp vào cơ sở dữ liệu để tránh race condition
            boolean success = DataManager.getInstance().addBidderBalance(bidder.getId(), amount);
            if (success) {
                DataManager.getInstance().markBidderToppedUp(bidder.getId());
                // Đồng bộ số dư thực tế mới nhất từ DB vào RAM
                double newBalance = DataManager.getInstance().getBidderBalance(bidder.getId());
                bidder.setBalance(newBalance);
                bidder.setLastTopUpTime(java.time.LocalDateTime.now());
                return Response.ok(bidder.getBalance());
            } else {
                return Response.error("Không thể cập nhật số dư vào cơ sở dữ liệu");
            }
        } catch (Exception e) {
            return Response.error("Lỗi nạp tiền: " + e.getMessage());
        }
    }

    private static Response handleSetAutoBid(Request request, ClientHandler handler) {
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
            return Response.error("maxBid phải lớn hơn giá hiện tại ($" + auction.getCurrentHighestBid() + ")");
        if (p.increment < auction.getMinBidStep())
            return Response.error("Bước tăng phải >= minBidStep ($" + auction.getMinBidStep() + ")");
        AutoBidConfig cfg = new AutoBidConfig(bidder.getId(), bidder.getUsername(), p.maxBid, p.increment);
        auction.registerAutoBid(cfg);
        return Response.ok("Đăng ký Auto-Bid thành công (tối đa $" + p.maxBid + ")");
    }

    private static Response handleCancelAutoBid(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        int auctionId = (Integer) request.getPayload();
        Auction auction = AuctionManager.getInstance().findAuctionById(auctionId);
        if (auction != null) auction.cancelAutoBid(user.getId());
        return Response.ok("Đã hủy Auto_Bid");
    }

    private static Response handleChangePassword(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) {
            return Response.error("Bạn cần đăng nhập để đổi mật khẩu!");
        }
        if (request.getPayload() == null || !(request.getPayload() instanceof ChangePasswordPayload)) {
            return Response.error("Dữ liệu đổi mật khẩu không hợp lệ!");
        }
        ChangePasswordPayload p = (ChangePasswordPayload) request.getPayload();

        // So khớp mật khẩu cũ
        if (!com.auction.security.PasswordUtil.verify(p.oldPassword, user.getPassword())) {
            return Response.error("Mật khẩu cũ không chính xác!");
        }

        // Thực hiện đổi mật khẩu
        boolean ok = UserManager.getInstance().updatePassword(user, p.newPassword);
        if (ok) {
            return Response.ok("Đổi mật khẩu thành công!");
        } else {
            return Response.error("Không thể cập nhật mật khẩu mới!");
        }
    }

    /**
     * Xác nhận thanh toán sau khi Bidder thắng phiên đấu giá.
     * Luồng:
     * 1. Kiểm tra quyền (Bidder, đã đăng nhập, là người thắng).
     * 2. Gọi auction.markPaid() → chuyển trạng thái RAM sang PAID.
     * 3. Cập nhật DB: auction → PAID, item → SOLD (transaction).
     * 4. Ghi nhận bidder_won + cộng doanh thu Seller.
     * 5. Gửi notification thành công cho Bidder.
     * 6. Gửi notification cho Seller (nếu online).
     * 7. Broadcast AUCTION_ENDED để tất cả client reload card.
     */
    private static Response handleConfirmPayment(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) {
            return Response.error("Bạn cần đăng nhập để thanh toán!");
        }
        if (!(user instanceof Bidder)) {
            return Response.error("Chỉ người mua (Bidder) mới có thể thực hiện thanh toán!");
        }
        if (!(request.getPayload() instanceof Integer auctionId)) {
            return Response.error("Dữ liệu thanh toán không hợp lệ!");
        }

        Auction auction = AuctionManager.getInstance().findAuctionById(auctionId);
        if (auction == null) {
            return Response.error("Phiên đấu giá không tồn tại!");
        }
        if (auction.getStatus() != com.auction.model.auction.AuctionStatus.FINISHED) {
            return Response.error("Phiên đấu giá chưa kết thúc hoặc đã được thanh toán!");
        }
        if (auction.getCurrentWinner() == null || auction.getCurrentWinner().getId() != user.getId()) {
            return Response.error("Bạn không phải người thắng cuộc của phiên này!");
        }

        try {
            Bidder bidder = (Bidder) user;

            // 1. Cập nhật trạng thái trong RAM
            auction.markPaid();

            // 2. Cập nhật DB: auction → PAID, item → SOLD
            boolean dbOk = AuctionManager.getInstance().payAuction(auctionId);
            if (!dbOk) {
                // Rollback RAM
                auction.restoreStatus(com.auction.model.auction.AuctionStatus.FINISHED);
                return Response.error("Không thể cập nhật trạng thái thanh toán vào cơ sở dữ liệu!");
            }

            // 3. Ghi nhận bidder thắng
            DataManager.getInstance().saveBidderWon(bidder.getId(), auctionId);
            if (bidder.getProfile() != null) {
                bidder.getProfile().addWonAuction(auctionId);
            }

            // 4. Cộng doanh thu cho Seller
            int sellerId = auction.getItem().getOwnerId();
            DataManager.getInstance().updateSellerRevenue(sellerId, auction.getCurrentHighestBid());

            // 5. Thông báo thành công cho Bidder
            String successMsg = String.format(
                    "Bạn đã thanh toán thành công $%,.2f cho sản phẩm [%s] — Phiên #%d.",
                    auction.getCurrentHighestBid(), auction.getItem().getName(), auctionId);
            handler.sendNotification(new Notification("PAYMENT_SUCCESS", successMsg));

            // 6. Thông báo cho Seller (nếu đang online)
            if (com.auction.server.AuctionServer.getInstance() != null) {
                String sellerMsg = String.format(
                        "💰 [%s] đã thanh toán $%,.2f cho sản phẩm [%s] — Phiên #%d.",
                        bidder.getUsername(), auction.getCurrentHighestBid(),
                        auction.getItem().getName(), auctionId);
                for (ClientHandler client : com.auction.server.AuctionServer.getInstance().getConnectedClients()) {
                    User u = client.getLoggedInUser();
                    if (u != null && u.getId() == sellerId) {
                        client.sendNotification(new Notification("PAYMENT_RECEIVED", sellerMsg));
                        break;
                    }
                }
            }

            // 7. Broadcast AUCTION_ENDED để tất cả client reload card
            if (com.auction.server.AuctionServer.getInstance() != null) {
                com.auction.server.AuctionServer.getInstance().broadcast(
                        new Notification("AUCTION_ENDED",
                                String.format("Phiên #%d [%s] đã được thanh toán.", auctionId, auction.getItem().getName())));
            }

            return Response.ok("Thanh toán thành công!");

        } catch (IllegalStateException e) {
            return Response.error("Lỗi trạng thái: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[handleConfirmPayment] Lỗi: " + e.getMessage());
            return Response.error("Lỗi xử lý thanh toán: " + e.getMessage());
        }
    }

    private static Response handleDeclinePayment(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) {
            return Response.error("Bạn cần đăng nhập để từ chối thanh toán!");
        }
        if (!(user instanceof Bidder)) {
            return Response.error("Chỉ người mua (Bidder) mới có thể thực hiện từ chối thanh toán!");
        }
        if (!(request.getPayload() instanceof Integer auctionId)) {
            return Response.error("Dữ liệu không hợp lệ!");
        }

        Auction auction = AuctionManager.getInstance().findAuctionById(auctionId);
        if (auction == null) {
            return Response.error("Phiên đấu giá không tồn tại!");
        }
        if (auction.getStatus() != com.auction.model.auction.AuctionStatus.FINISHED) {
            return Response.error("Chỉ có thể từ chối thanh toán khi phiên ở trạng thái FINISHED!");
        }
        if (auction.getCurrentWinner() == null || auction.getCurrentWinner().getId() != user.getId()) {
            return Response.error("Bạn không phải người thắng cuộc của phiên này!");
        }

        try {
            // Hủy phiên đấu giá do người mua từ chối thanh toán
            boolean ok = AuctionManager.getInstance().cancelAuctionByAdmin(auctionId, "Người mua từ chối thanh toán.");
            if (ok) {
                // Gửi thông báo đến Seller (nếu online)
                int sellerId = auction.getItem().getOwnerId();
                if (com.auction.server.AuctionServer.getInstance() != null) {
                    String sellerMsg = String.format(
                            "❌ Người thắng [%s] đã từ chối thanh toán cho sản phẩm [%s] — Phiên #%d. Phiên đấu giá bị hủy.",
                            user.getUsername(), auction.getItem().getName(), auctionId);
                    for (ClientHandler client : com.auction.server.AuctionServer.getInstance().getConnectedClients()) {
                        User u = client.getLoggedInUser();
                        if (u != null && u.getId() == sellerId) {
                            client.sendNotification(new Notification("PAYMENT_DECLINED", sellerMsg));
                            break;
                        }
                    }
                }
                return Response.ok("Đã từ chối thanh toán và hủy phiên thành công!");
            } else {
                return Response.error("Không thể xử lý từ chối thanh toán.");
            }
        } catch (Exception e) {
            return Response.error("Lỗi: " + e.getMessage());
        }
    }

    private static Response handleVerifyEmail(Request request) {
        if (request.getPayload() == null || !(request.getPayload() instanceof String)) {
            return Response.error("Email không hợp lệ!");
        }
        String email = (String) request.getPayload();
        User user = UserManager.getInstance().findUserByEmail(email);
        if (user != null) {
            return Response.ok(user);
        } else {
            return Response.error("Email không tồn tại trên hệ thống!");
        }
    }

    private static Response handleResetPassword(Request request) {
        if (request.getPayload() == null || !(request.getPayload() instanceof ResetPasswordPayload)) {
            return Response.error("Dữ liệu khôi phục mật khẩu không hợp lệ!");
        }
        ResetPasswordPayload p = (ResetPasswordPayload) request.getPayload();
        User user = UserManager.getInstance().findUser(p.username);
        if (user == null) return Response.error("Tên đăng nhập không tồn tại!");

        boolean ok = UserManager.getInstance().updatePassword(user, p.newPassword);
        if (ok) return Response.ok("Đổi mật khẩu thành công!");
        else return Response.error("Không thể cập nhật mật khẩu mới!");
    }

    // =========================================================================
    // --- BẮT ĐẦU CÁC HÀM XỬ LÝ DÀNH CHO ADMIN (ADMIN HANDLERS) ---
    // =========================================================================

    private static Response handleAdminGetAllUsers(ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        return Response.ok(UserManager.getInstance().getAllUsers());
    }

    private static Response handleAdminBanUser(Request request, ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        boolean ok = UserManager.getInstance().banUser(p.targetId, p.reason);
        if (ok) {
            // Tìm và ngắt kết nối luôn nếu user bị ban đang online
            ClientHandler targetClient = handler.getServer().getConnectedClients().stream()
                    .filter(c -> c.getLoggedInUser() != null && c.getLoggedInUser().getId() == p.targetId)
                    .findFirst().orElse(null);
            if (targetClient != null) {
                targetClient.sendNotification(new Notification("FORCE_LOGOUT", "Tài khoản của bạn đã bị khóa bởi Admin với lý do: " + p.reason));
                targetClient.setLoggedInUser(null);
            }
            return Response.ok("Đã khóa tài khoản thành công!");
        }
        return Response.error("Không thể khóa tài khoản hoặc tài khoản không tồn tại.");
    }

    private static Response handleAdminUnbanUser(Request request, ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        boolean ok = UserManager.getInstance().unbanUser(p.targetId);
        if (ok) return Response.ok("Đã mở khóa tài khoản thành công!");
        return Response.error("Không thể mở khóa tài khoản.");
    }

    private static Response handleAdminDeleteUser(Request request, ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        try {
            boolean ok = UserManager.getInstance().deleteUserPermanently(p.targetId);
            if (ok) return Response.ok("Đã xóa vĩnh viễn tài khoản khỏi hệ thống!");
            return Response.error("Xóa tài khoản thất bại.");
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    private static Response handleAdminResetUserPassword(Request request, ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        User user = UserManager.getInstance().findUserById(p.targetId);
        if (user == null) return Response.error("Không tìm thấy user mục tiêu.");

        boolean ok = UserManager.getInstance().updatePassword(user, p.newPassword);
        if (ok) return Response.ok("Đã đặt lại mật khẩu cho user thành công!");
        return Response.error("Không thể đặt lại mật khẩu.");
    }

    private static Response handleAdminGetAllAuctions(ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        return Response.ok(AuctionManager.getInstance().getAllAuctionsIncludingInactive());
    }

    private static Response handleAdminCancelAuction(Request request, ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        boolean ok = AuctionManager.getInstance().cancelAuctionByAdmin(p.targetId, p.reason);
        if (ok) {
            handler.getServer().broadcast(new Notification("AUCTION_CANCELED_BY_ADMIN", "Phiên đấu giá #" + p.targetId + " đã bị hủy bởi Admin."));
            return Response.ok("Đã hủy phiên đấu giá thành công!");
        }
        return Response.error("Không thể hủy phiên đấu giá.");
    }

    private static Response handleAdminGetAllItems(ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        return Response.ok(ItemManager.getInstance().getAllItemsInSystem());
    }

    private static Response handleAdminForceDeleteItem(Request request, ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        boolean ok = ItemManager.getInstance().forceDeleteItemByAdmin(p.targetId);
        if (ok) return Response.ok("Admin đã xóa cưỡng chế sản phẩm thành công!");
        return Response.error("Không thể xóa sản phẩm.");
    }

    private static Response handleAdminGetStats(ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        // Bạn có thể trả về một Object chứa map hoặc chuỗi tổng hợp thống kê không định dạng String.format như trước
        return Response.ok(DataManager.getInstance().getSystemStatisticsDto());
    }

    private static Response handleAdminGetAuditLog(ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        return Response.ok(DataManager.getInstance().getSecurityAuditLogs());
    }

    private static Response handleAdminGetActiveSessions(ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");

        // Thu thập thông tin các Session đang kết nối
        List<String> activeSessions = handler.getServer().getConnectedClients().stream()
                .map(client -> {
                    User u = client.getLoggedInUser();
                    return "Session ID: " + client.hashCode() + " | User: " + (u != null ? u.getUsername() + " (" + u.getRole() + ")" : "GUEST");
                })
                .toList();
        return Response.ok(activeSessions);
    }

    private static Response handleAdminKickUser(Request request, ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        ClientHandler targetClient = handler.getServer().getConnectedClients().stream()
                .filter(c -> c.getLoggedInUser() != null && c.getLoggedInUser().getId() == p.targetId)
                .findFirst().orElse(null);

        if (targetClient != null) {
            targetClient.sendNotification(new Notification("KICKED", "Bạn đã bị sút ra khỏi hệ thống bởi Admin."));
            targetClient.closeConnection(); // Hoặc logic ngắt kết nối socket của dự án
            return Response.ok("Đã đá user thành công.");
        }
        return Response.error("User hiện không online hoặc không tìm thấy session.");
    }
}