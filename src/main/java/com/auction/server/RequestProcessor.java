package com.auction.server;

import com.auction.data.DataManager;
import com.auction.manager.AuctionManager;
import com.auction.manager.ItemManager;
import com.auction.manager.UserManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.AutoBidConfig;
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
import com.auction.security.AuditLogger;
import com.auction.security.InputValidator;
import com.auction.security.PasswordUtil;
import com.auction.security.SessionManager;
import com.auction.server.handler.*;

import java.time.LocalDateTime;
import java.util.List;

import static com.auction.server.handler.AdminHandler.audit;
import static com.auction.server.handler.AuctionHandler.BID_THROTTLE_MS;
import static com.auction.server.handler.AuctionHandler.lastBidTime;
import static com.auction.server.handler.AuthHandler.rateLimiter;

public class RequestProcessor {

    public static Response process(Request request, ClientHandler handler) {
        try {
            return switch (request.getType()) {
                // Auth
                case LOGIN           -> AuthHandler.handleLogin(request, handler);
                case REGISTER        -> AuthHandler.handleRegister(request, handler);
                case LOGOUT          -> AuthHandler.handleLogout(handler);
                case VERIFY_EMAIL    -> AuthHandler.handleVerifyEmail(request);
                case RESET_PASSWORD  -> AuthHandler.handleResetPassword(request);
                case CHANGE_PASSWORD -> AuthHandler.handleChangePassword(request, handler);

                // Auction
                case GET_AUCTIONS       -> AuctionHandler.handleGetAuctions();
                case GET_AUCTION_DETAIL -> AuctionHandler.handleGetAuctionDetail(request, handler);
                case PLACE_BID          -> AuctionHandler.handlePlaceBid(request, handler);
                case CREATE_AUCTION     -> AuctionHandler.handleCreateAuction(request, handler);
                case SET_AUTO_BID       -> AuctionHandler.handleSetAutoBid(request, handler);
                case CANCEL_AUTO_BID    -> AuctionHandler.handleCancelAutoBid(request, handler);

                // Item
                case CREATE_ITEM  -> ItemHandler.handleCreateItem(request, handler);
                case GET_MY_ITEMS -> ItemHandler.handleGetMyItems(handler);
                case DELETE_ITEM  -> ItemHandler.handleDeleteItem(request, handler);
                case UPDATE_ITEM  -> ItemHandler.handleUpdateItem(request, handler);

                // Bidder
                case ADD_TO_WATCHLIST      -> BidderHandler.handleAddToWatchlist(request, handler);
                case REMOVE_FROM_WATCHLIST -> BidderHandler.handleRemoveFromWatchlist(request, handler);
                case TOP_UP                -> BidderHandler.handleTopUp(request, handler);
                case CONFIRM_PAYMENT       -> BidderHandler.handleConfirmPayment(request, handler);
                case DECLINE_PAYMENT       -> BidderHandler.handleDeclinePayment(request, handler);

                // Admin
                case ADMIN_GET_ALL_USERS       -> AdminHandler.handleGetAllUsers(handler);
                case ADMIN_BAN_USER            -> AdminHandler.handleBanUser(request, handler);
                case ADMIN_UNBAN_USER          -> AdminHandler.handleUnbanUser(request, handler);
                case ADMIN_UNBAN_USERS_BATCH   -> AdminHandler.handleUnbanUsersBatch(request, handler);
                case ADMIN_DELETE_USER         -> AdminHandler.handleDeleteUser(request, handler);
                case ADMIN_RESET_USER_PASSWORD -> AdminHandler.handleResetUserPassword(request, handler);
                case ADMIN_GET_ALL_AUCTIONS    -> AdminHandler.handleGetAllAuctions(handler);
                case ADMIN_CANCEL_AUCTION      -> AdminHandler.handleCancelAuction(request, handler);
                case ADMIN_GET_ALL_ITEMS       -> AdminHandler.handleGetAllItems(handler);
                case ADMIN_FORCE_DELETE_ITEM   -> AdminHandler.handleForceDeleteItem(request, handler);
                case ADMIN_APPROVE_ITEM        -> AdminHandler.handleApproveItem(request, handler);
                case ADMIN_APPROVE_ITEMS_BATCH -> AdminHandler.handleApproveItemsBatch(request, handler);
                case ADMIN_GET_STATS           -> AdminHandler.handleGetStats(handler);
                case ADMIN_GET_AUDIT_LOG       -> AdminHandler.handleGetAuditLog(handler);
                case ADMIN_GET_ACTIVE_SESSIONS -> AdminHandler.handleGetActiveSessions(handler);
                case ADMIN_KICK_USER           -> AdminHandler.handleKickUser(request, handler);
            };
        } catch (Exception e) {
            System.err.println("[RequestProcessor] Unhandled: " + request.getType() + " → " + e.getMessage());
            return Response.error("Lỗi xử lý yêu cầu: " + e.getMessage());
        }
    }

    // Kiểm tra xem User hiện tại có phải là Admin hay không
    private static boolean isAdmin(User user) {
        if (user == null) return false;
        // Bạn có thể check qua user.getRole().equals("ADMIN") hoặc tùy cấu trúc thuộc tính Role của bạn
        return "ADMIN".equalsIgnoreCase(user.getRole());
    }
    private static Response requireAdmin(ClientHandler handler) {
        User u = handler.getLoggedInUser();
        if (u == null) return Response.error("Bạn cần đăng nhập!");
        if (!isAdmin(u)) return Response.error("Từ chối: Quyền Admin là bắt buộc!");
        return null; // ok
    }

    // kiểm tra danh tính
    private static Response handleLogin(Request request, ClientHandler handler) {
        // ── Security 1: RateLimiter ──
        String ip = handler.getClientIp();

        String blocked = rateLimiter.check(ip);
        if (blocked != null) {
            audit.logIpBlocked(ip);
            return Response.error(blocked);
        }
        // Kiểm tra payload hợp lệ
        if (request.getPayload() == null || !(request.getPayload() instanceof LoginPayload)) {
            return Response.error("Dữ liệu đăng nhập không hợp lệ!");
        }
        LoginPayload payload = (LoginPayload) request.getPayload();
// ── Security 2: InputValidator ──
        if (!InputValidator.isValidUsername(payload.username)) {
            rateLimiter.recordFailure(ip);
            return Response.error("Tên đăng nhập không hợp lệ!");
        }
        try {
            User user = UserManager.getInstance().login(payload.username, payload.password);
            if (user == null) {
                return Response.error("Tên đăng nhập hoặc mật khẩu không đúng!");
            }

            // Kiểm tra xem tài khoản này đã đăng nhập ở một kết nối khác chưa
            if (AuctionServer.getInstance() != null) {
                for (ClientHandler existingClient : AuctionServer.getInstance().getConnectedClients()) {
                    if (existingClient == handler) continue; // bỏ qua chính handler này
                    User loggedUser = existingClient.getLoggedInUser();
                    if (loggedUser != null && loggedUser.getId() == user.getId()) {
                        return Response.error("Tài khoản đang hoạt động trên một thiết bị khác. Vui lòng đăng xuất thiết bị đó trước!");
                    }
                }
            }


            if (!user.isActive()) {
                if ("PENDING".equalsIgnoreCase(user.getStatus())) {
                    return Response.error("Tài khoản của bạn đang chờ duyệt từ Admin. Vui lòng quay lại sau!");
                }
                return Response.error("Tài khoản của bạn đã bị khóa! Lý do: " + user.getBanReason());
            }
            rateLimiter.recordSuccess(ip);
            audit.logLoginSuccess(ip, payload.username);

            // ── Security 4: Tạo session token ──
            String token = SessionManager.getInstance().createSession(user.getId(), user.getRole());
            handler.setSessionToken(token);
            handler.setLoggedInUser(user);
            // lưu User vào handler để biết ai đang gửi request
            handler.setLoggedInUser(user);
            return Response.ok(user);
        } catch (Exception e) {
            return Response.error("Đăng nhập thất bại: " + e.getMessage());
        }
    }


    // đăng kí tài khoản mới
    private static Response handleRegister(Request request, ClientHandler handler) {
        String ip = handler.getClientIp();

        // ── Security 1: RateLimiter ──
        String blocked = rateLimiter.check(ip);
        if (blocked != null) {
            audit.logIpBlocked(ip);
            return Response.error(blocked);
        }

        if (!(request.getPayload() instanceof RegisterPayload p))
            return Response.error("Dữ liệu đăng ký không hợp lệ!");

        // ── Security 2: Validate toàn bộ input ──
        if (!InputValidator.isValidUsername(p.username))
            return Response.error("Username chỉ gồm chữ, số, dấu _ và dài 3–30 ký tự!");

        String pwErr = InputValidator.getPasswordError(p.password);
        if (pwErr != null) return Response.error(pwErr);

        if (!InputValidator.isValidEmail(p.email))
            return Response.error("Email không hợp lệ!");

        if (!InputValidator.isValidRole(p.role))
            return Response.error("Vai trò không hợp lệ! Chỉ chấp nhận BIDDER hoặc SELLER.");

        try {
            User user = UserManager.getInstance().register(p.username, p.password, p.email, p.role);
            if (user == null && InputValidator.isValidEmail(p.email)) return Response.error("Đăng ký thất bại, vui lòng thử lại.");
            audit.logRegister(ip, p.username);
            return Response.ok(user);
        } catch (IllegalArgumentException e) {
            rateLimiter.recordFailure(ip);
            return Response.error(e.getMessage());
        } catch (Exception e) {
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
            // ── Security: hủy session token ──
            if (handler.getSessionToken() != null) {
                SessionManager.getInstance().invalidate(handler.getSessionToken());
                handler.setSessionToken(null);
            }
            handler.setLoggedInUser(null);
            return Response.ok("Đăng xuất thành công");
        } catch (Exception e) {
            return Response.error("Lỗi đăng xuất: " + e.getMessage());
        }
    }

    // lấy danh sách
    private static Response handleGetAuctions() {
        // Lấy danh sách từ RAM (đã được load từ file .ser lúc khởi động server)
        return Response.ok(AuctionManager.getInstance().getAuctions());
    }

    // xử lý việc trả giá
    private static Response handlePlaceBid(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập để đấu giá!");
        if (!(user instanceof Bidder bidder)) return Response.error("Chỉ Bidder mới có quyền đặt giá!");
        if (!(request.getPayload() instanceof PlaceBidPayload payload))
            return Response.error("Dữ liệu đặt giá không hợp lệ!");

        // ── Security: throttle chống spam bid ──
        long now = System.currentTimeMillis();
        Long last = lastBidTime.get(user.getId());
        if (last != null && now - last < BID_THROTTLE_MS)
            return Response.error("Bạn đang gửi yêu cầu quá nhanh! Vui lòng đợi.");
        lastBidTime.put(user.getId(), now);

        // ── Security: validate số tiền ──
        if (payload.amount <= 0) return Response.error("Số tiền đặt giá phải lớn hơn 0!");

        try {
            Auction auction = AuctionManager.getInstance().findAuctionById(payload.auctionId);
            if (auction == null) return Response.error("Phiên đấu giá không tồn tại!");

            auction.placeBid(bidder, payload.amount);
            bidder.getProfile().addParticipatedAuction(payload.auctionId);

            BidUpdateData upd = new BidUpdateData(
                    payload.auctionId, auction.getCurrentHighestBid(),
                    bidder.getUsername(), auction.getBidHistory().size(), auction.getEndTime());
            handler.getServer().broadcast(new Notification("BID_UPDATE", upd));
            audit.logBid(handler.getClientIp(), user.getId(), payload.auctionId, payload.amount);
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
                for (Observer obs : a.getObservers()) {
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
        if (!(request.getPayload() instanceof CreateItemPayload p)) return Response.error("Payload không hợp lệ!");
        if (p.startingPrice < 100) return Response.error("Giá khởi điểm tối thiểu là 100!");

        // ── Security: sanitize name & description ──
        String name = InputValidator.sanitize(p.name);
        String desc = InputValidator.sanitize(p.description);
        if (name.isBlank()) return Response.error("Tên sản phẩm không hợp lệ!");

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
        if (!p.endTime.isAfter(LocalDateTime.now()))
            return Response.error("Thời gian kết thúc phải ở trong tương lai");

        Item item = ItemManager.getInstance().findItem(p.itemId);
        if (item == null) return Response.error("Sản phẩm không tồn tại: " + p.itemId);
        if (item.getOwnerId() != user.getId()) return Response.error("Bạn không phải chủ sở hữu sản phẩm này");
        Auction auction = AuctionManager.getInstance().createAuction(item, p.startTime, p.endTime, p.minBidStep);
        if (auction == null) return Response.error("Không thể tạo phiên — sản phẩm không ở trạng thái AVAILABLE");

        // Chỉ kích hoạt (start) phiên đấu giá nếu thời gian bắt đầu đã đến hoặc ở quá khứ và vật phẩm đã được duyệt
        LocalDateTime now = LocalDateTime.now();
        if (item.getStatus() != ItemStatus.PENDING && (p.startTime == null || !p.startTime.isAfter(now))) {
            auction.start();
            DataManager.getInstance().startAuction(auction.getId());
        }
        handler.sendNotification(new Notification("AUCTION_CREATED", String.format("Phiên đấu giá cho sản phẩm [%s] đã được tạo thành công!", item.getName())));

        // Gửi thông báo đấu giá mới cho tất cả các client là Bidder chỉ khi sản phẩm đã được duyệt (không ở trạng thái PENDING)
        if (item.getStatus() != ItemStatus.PENDING) {
            for (ClientHandler client : handler.getServer().getConnectedClients()) {
                User u = client.getLoggedInUser();
                if (u instanceof Bidder) {
                    client.sendNotification(new Notification("NEW_AUCTION", String.format("Sản phẩm mới [%s] vừa lên sàn đấu giá! Hãy tham gia ngay.", item.getName())));
                }
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

        if (item.getStatus() == ItemStatus.IN_AUCTION) {
            Auction auction = AuctionManager.getInstance().getAuctions().stream()
                    .filter(a -> a.getItem() != null && a.getItem().getId() == item.getId())
                    .findFirst().orElse(null);
            if (auction == null || auction.getStatus() != AuctionStatus.OPEN) {
                return Response.error("Không thể xóa sản phẩm đang ở trạng thái IN_AUCTION");
            }
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
            Auction auction = AuctionManager.getInstance().getAuctions().stream()
                    .filter(a -> a.getItem().getId() == existingItem.getId())
                    .findFirst().orElse(null);
            if (auction != null && auction.getStatus() != AuctionStatus.OPEN) {
                return Response.error("Chỉ có thể sửa sản phẩm khi phiên đấu giá chưa bắt đầu (OPEN).");
            }
        } else if (existingItem.getStatus() != ItemStatus.AVAILABLE && existingItem.getStatus() != ItemStatus.PENDING) {
            return Response.error("Chỉ có thể sửa sản phẩm ở trạng thái AVAILABLE, PENDING hoặc phiên chưa bắt đầu.");
        }

        ItemFactory factory = switch (p.type.toUpperCase()) {
            case "ART" -> new ArtFactory(p.artist);
            case "VEHICLE" -> new VehicleFactory(p.year);
            default -> new ElectronicsFactory(p.brand);
        };

        // Tạo item mới dựa trên factory để thay thế
        Item updatedItem = factory.createItem(p.id, user.getId(), p.name, p.description, p.startingPrice, p.imageUrl);
        updatedItem.setStatus(existingItem.getStatus());
        // ── Security: sanitize ──
        String name = InputValidator.sanitize(p.name);
        String desc = InputValidator.sanitize(p.description);

        boolean success = ItemManager.getInstance().updateItem(updatedItem);
        if (success) {
            Auction auction = AuctionManager.getInstance().getAuctions().stream()
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

            // Cộng dồn trực tiếp vào cơ sở dữ liệu để tránh race condition
            boolean success = DataManager.getInstance().addBidderBalance(bidder.getId(), amount);
            if (success) {
                DataManager.getInstance().markBidderToppedUp(bidder.getId());
                // Đồng bộ số dư thực tế mới nhất từ DB vào RAM
                double newBalance = DataManager.getInstance().getBidderBalance(bidder.getId());
                bidder.setBalance(newBalance);
                bidder.setLastTopUpTime(LocalDateTime.now());
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
        if (auction.getStatus() != AuctionStatus.RUNNING)
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
        if (!PasswordUtil.verify(p.oldPassword, user.getPassword())) {
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
        if (auction.getStatus() != AuctionStatus.FINISHED) {
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
                auction.restoreStatus(AuctionStatus.FINISHED);
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
            if (AuctionServer.getInstance() != null) {
                String sellerMsg = String.format(
                        "💰 [%s] đã thanh toán $%,.2f cho sản phẩm [%s] — Phiên #%d.",
                        bidder.getUsername(), auction.getCurrentHighestBid(),
                        auction.getItem().getName(), auctionId);
                for (ClientHandler client : AuctionServer.getInstance().getConnectedClients()) {
                    User u = client.getLoggedInUser();
                    if (u != null && u.getId() == sellerId) {
                        client.sendNotification(new Notification("PAYMENT_RECEIVED", sellerMsg));
                        break;
                    }
                }
            }

            // 7. Broadcast AUCTION_ENDED để tất cả client reload card
            if (AuctionServer.getInstance() != null) {
                AuctionServer.getInstance().broadcast(
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
        if (auction.getStatus() != AuctionStatus.FINISHED) {
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
                if (AuctionServer.getInstance() != null) {
                    String sellerMsg = String.format(
                            "❌ Người thắng [%s] đã từ chối thanh toán cho sản phẩm [%s] — Phiên #%d. Phiên đấu giá bị hủy.",
                            user.getUsername(), auction.getItem().getName(), auctionId);
                    for (ClientHandler client : AuctionServer.getInstance().getConnectedClients()) {
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
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Payload không hợp lệ!");

        boolean ok = UserManager.getInstance().banUser(p.targetId, p.reason);
        if (ok) {
            audit.logAdminBan(handler.getClientIp(), handler.getLoggedInUser().getId(), p.targetId, p.reason);
            // Kick nếu đang online
            handler.getServer().getConnectedClients().stream()
                    .filter(c -> c.getLoggedInUser() != null && c.getLoggedInUser().getId() == p.targetId)
                    .findFirst().ifPresent(c -> {
                        c.sendNotification(new Notification("FORCE_LOGOUT",
                                "Tài khoản bị khóa bởi Admin. Lý do: " + p.reason));
                        c.setLoggedInUser(null);
                    });
            return Response.ok("Đã khóa tài khoản thành công!");
        }
        return Response.error("Không thể khóa tài khoản.");
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

    private static Response handleAdminApproveItem(Request request, ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        Item item = ItemManager.getInstance().findItem(p.targetId);
        if (item == null) return Response.error("Sản phẩm không tồn tại.");

        Auction auction = AuctionManager.getInstance().getAuctions().stream()
                .filter(a -> a.getItem() != null && a.getItem().getId() == p.targetId)
                .findFirst().orElse(null);

        boolean ok;
        if (auction != null) {
            LocalDateTime now = LocalDateTime.now();
            if (auction.getStartTime() == null || !auction.getStartTime().isAfter(now)) {
                if (auction.getStatus() == AuctionStatus.OPEN) {
                    auction.start();
                    DataManager.getInstance().startAuction(auction.getId());
                }
                ok = ItemManager.getInstance().updateItemStatus(p.targetId, ItemStatus.IN_AUCTION);
            } else {
                auction.restoreStatus(com.auction.model.auction.AuctionStatus.OPEN);
                ok = ItemManager.getInstance().updateItemStatus(p.targetId, ItemStatus.AVAILABLE);
            }
        } else {
            ok = ItemManager.getInstance().updateItemStatus(p.targetId, ItemStatus.AVAILABLE);
        }

        if (ok) {
            if (AuctionServer.getInstance() != null) {
                String msg = String.format("Sản phẩm [%s] của bạn đã được Admin phê duyệt!", item.getName());
                for (ClientHandler client : AuctionServer.getInstance().getConnectedClients()) {
                    User u = client.getLoggedInUser();
                    if (u != null && u.getId() == item.getOwnerId()) {
                        client.sendNotification(new Notification("PRODUCT_APPROVED", msg));
                        break;
                    }
                }

                AuctionServer.getInstance().broadcast(new Notification("ITEM_STATUS_CHANGED", String.valueOf(p.targetId)));

                if (auction != null) {
                    for (ClientHandler client : AuctionServer.getInstance().getConnectedClients()) {
                        User u = client.getLoggedInUser();
                        if (u instanceof Bidder) {
                            client.sendNotification(new Notification("NEW_AUCTION", String.format("Sản phẩm mới [%s] vừa lên sàn đấu giá! Hãy tham gia ngay.", item.getName())));
                        }
                    }
                }
            }
            return Response.ok("Duyệt sản phẩm thành công!");
        }
        return Response.error("Không thể duyệt sản phẩm.");
    }

    private static Response handleAdminGetStats(ClientHandler handler) {
        if (!isAdmin(handler.getLoggedInUser())) return Response.error("Từ chối truy cập: Quyền Admin là bắt buộc!");
        // Bạn có thể trả về một Object chứa map hoặc chuỗi tổng hợp thống kê không định dạng String.format như trước
        return Response.ok(DataManager.getInstance().getSystemStatisticsDto());
    }

    private static Response handleAdminGetAuditLog(ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        // ── Security: chỉ Admin mới được xem audit log ──
        List<String> logs = AuditLogger.getInstance().getRecentLogs(200);
        return Response.ok(logs);
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