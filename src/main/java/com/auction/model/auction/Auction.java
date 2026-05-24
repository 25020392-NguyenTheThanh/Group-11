package com.auction.model.auction;

import com.auction.data.DataManager;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.pattern.observer.Observer;
import com.auction.pattern.observer.Subject;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.auction.model.auction.AutoBidConfig;
import com.auction.server.ClientHandler;
import com.auction.manager.UserManager;
import com.auction.manager.AuctionManager;
import com.auction.network.Notification;

// Auction đóng vai trò là Subject — notify tất cả observer khi có thay đổi
// sửa thêm một số chỗ tránh race condition!!

public class Auction implements Subject, Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private Item item;
    private double currentHighestBid;
    private Bidder currentWinner;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<BidTransaction> bidHistory;
    private transient CopyOnWriteArrayList<Observer> observers; // không lưu vào file
    private final double minBidStep; // bước giá tối thiểu
    private int viewCount; // số lượt xem
    private Map<Integer , AutoBidConfig> autoBidConfigs = new ConcurrentHashMap<>();

    // Anti-sniping: ngưỡng giây cuối mà nếu có bid sẽ gia hạn
    private static final long SNIPE_WINDOW_SECONDS = 30;   // bid trong 30s cuối → gia hạn
    private static final long EXTENSION_SECONDS    = 60;   // gia hạn thêm 60s
    private int extensionCount = 0;                        // số lần đã gia hạn
    private static final int MAX_EXTENSIONS        = 5;    // tối đa 5 lần

    public Auction(int id, Item item, LocalDateTime startTime, LocalDateTime endTime, double minBidStep) {
        this.id = id;
        this.item = item;
        this.currentHighestBid = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minBidStep = minBidStep; // bước giá tối thiểu
        this.bidHistory = new ArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.viewCount = 0;
    }


    //  Observer (Subject interface)

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String message) {
        for (Observer o : observers) {
            o.send(message);
        }
    }

    //  Lifecycle

    public synchronized void start() {
        if (status != AuctionStatus.OPEN)
            throw new IllegalStateException("Chỉ có thể bắt đầu phiên ở trạng thái OPEN");

        status = AuctionStatus.RUNNING;

        String msg = String.format(
                "▶ Phiên #%d [%s] bắt đầu! Giá khởi điểm: $%,.0f | Bước giá tối thiểu: $%,.0f",
                id, item.getName(), currentHighestBid, minBidStep);
        System.out.println(msg);
        notifyObservers(msg);
    }

    public synchronized void finish() {
        if (status != AuctionStatus.RUNNING) {
            throw new IllegalStateException("Auction not running");
        }

        String msg;
        if (currentWinner == null) {
            status = AuctionStatus.CANCELED;
            msg = String.format(
                    "■ Phiên #%d [%s] kết thúc — Không có người đặt giá. Trạng thái: CANCELED.",
                    id, item.getName());
        } else {
            status = AuctionStatus.FINISHED;
            msg = String.format(
                    "■ Phiên #%d [%s] kết thúc — Người thắng: %s với giá $%,.0f",
                    id, item.getName(), currentWinner.getUsername(), currentHighestBid);
        }
        System.out.println(msg);
        notifyObservers(msg);
    }

    public synchronized void cancel(String reason) { // hủy phiên
        status = AuctionStatus.CANCELED;
        String msg = String.format(
                "✗ Phiên #%d [%s] bị hủy. Lý do: %s",
                id, item.getName(), reason);
        System.out.println(msg);
        notifyObservers(msg);
    }

    public synchronized void markPaid() { // Tự động gửi thông báo thanh toán thành công
        if (status != AuctionStatus.FINISHED)
            throw new IllegalStateException("Chỉ có thể thanh toán phiên FINISHED");

        status = AuctionStatus.PAID;
        String msg = String.format(
                "💰 Phiên #%d [%s] — %s đã thanh toán thành công $%,.0f",
                id, item.getName(), currentWinner.getUsername(), currentHighestBid);
        System.out.println(msg);
        notifyObservers(msg);
    }

    //  Đặt giá — synchronized chống race condition

    public synchronized void placeBid(Bidder bidder, double amount)
            throws InvalidBidException, AuctionClosedException {
        placeBid(bidder, amount, false);
    }

    public synchronized void placeBid(Bidder bidder, double amount, boolean isAutoBid)
            throws InvalidBidException, AuctionClosedException {
        if (!isAutoBid) {
            if (bidder == null || !bidder.isAuthenticated()) {
                throw new AuthenticationException("Người dùng chưa được xác thực");
            }
        }
        placeBidInternal(bidder, amount, isAutoBid);
        triggerAutoBid(bidder);
    }

    private void placeBidInternal(Bidder bidder, double amount, boolean isAutoBid)
            throws InvalidBidException, AuctionClosedException {

        // Kiểm tra số tiền
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }

        // Kiểm tra thời gian thực
        if (LocalDateTime.now().isAfter(endTime)) {
            if (status == AuctionStatus.RUNNING) {
                finish();
            }
            throw new AuctionClosedException("Phiên #" + id + " đã hết giờ.");
        }

        // Kiểm tra trạng thái phiên đấu giá
        if (status != AuctionStatus.RUNNING)
            throw new AuctionClosedException(String.format(
                    "Phiên #%d không ở trạng thái RUNNING (hiện: %s)", id, status));

        // Chặn đặt giá liên tiếp khi đang giữ giá cao nhất
        if (currentWinner != null && currentWinner.getId() == bidder.getId()) {
            throw new InvalidBidException("Bạn đã là người đặt giá cao nhất, không thể đặt giá tiếp.");
        }

        // Kiểm tra số dư
        if (bidder.getBalance() < amount)
            throw new InvalidBidException(String.format(
                    "Số dư không đủ — số dư hiện tại: $%,.0f, giá đặt: $%,.0f",
                    bidder.getBalance(), amount));

        double minAccepted = currentHighestBid + minBidStep;

        if (amount < minAccepted)
            throw new InvalidBidException(String.format(
                    "Giá đặt phải >= $%,.0f  (giá cao nhất $%,.0f + bước tối thiểu $%,.0f)",
                    minAccepted, currentHighestBid, minBidStep));

        // Hoàn tiền cho người ra giá cao nhất trước đó (nếu có)
        Bidder previousWinner = currentWinner;
        double previousHighestBid = currentHighestBid;
        if (currentWinner != null) {
            currentWinner.topUp(currentHighestBid);
            DataManager.getInstance().updateBidderBalance(currentWinner.getId(), currentWinner.getBalance());
        }

        // Khấu trừ tiền của new bidder
        bidder.deduct(amount);
        // Đồng bộ số dư mới của bidder xuống DB ngay lập tức
        DataManager.getInstance().updateBidderBalance(bidder.getId(), bidder.getBalance());

        // Lưu vào lịch sử
        BidTransaction tx = new BidTransaction(bidder.getId(), bidder.getUsername(), amount);
        bidHistory.add(tx);

        currentHighestBid = amount;
        currentWinner = bidder;

        // Thêm đấu giá này vào danh sách tham gia của bidder
        if (bidder.getProfile() != null) {
            bidder.getProfile().addParticipatedAuction(id);
        }

        // Ghi lịch sử bid vào MySQL
        try {
            AuctionManager.getInstance().recordBid(id, bidder.getId(), bidder.getUsername(), amount);
        } catch (Exception e) {
            System.err.println("Lỗi ghi lịch sử MySQL: " + e.getMessage());
        }

        checkAndExtend();

        // Chuẩn bị tin nhắn thông báo room
        String msg;
        if (isAutoBid) {
            msg = String.format("🔔 Phiên %d — Auto-Bid: %s vừa đặt $%,.2f | Giá cao nhất hiện tại: $%,.2f", id, bidder.getUsername(), amount, currentHighestBid);
        } else {
            msg = String.format("🔔 Phiên %d — %s vừa đặt $%,.2f | Giá cao nhất hiện tại: $%,.2f", id, bidder.getUsername(), amount, currentHighestBid);
        }
        System.out.println(msg);
        notifyObservers(msg);

        // Gửi thông báo đến bidder vừa thắng (nếu online)
        if (com.auction.server.AuctionServer.getInstance() != null) {
            for (ClientHandler client : com.auction.server.AuctionServer.getInstance().getConnectedClients()) {
                com.auction.model.user.User u = client.getLoggedInUser();
                if (u != null && u.getId() == bidder.getId()) {
                    client.sendNotification(new Notification("BALANCE_UPDATE", bidder.getBalance()));
                    String successMsg = isAutoBid 
                        ? String.format("Auto-Bid đặt giá thành công $%,.2f cho phiên #%d [%s].", amount, id, item.getName())
                        : String.format("Đặt giá thành công $%,.2f cho phiên #%d [%s].", amount, id, item.getName());
                    client.sendNotification(new Notification("BID_SUCCESS", successMsg));
                    break;
                }
            }
        }

        // Gửi thông báo đến người bị vượt mặt (nếu online)
        if (previousWinner != null && previousWinner.getId() != bidder.getId()) {
            if (com.auction.server.AuctionServer.getInstance() != null) {
                for (ClientHandler client : com.auction.server.AuctionServer.getInstance().getConnectedClients()) {
                    com.auction.model.user.User u = client.getLoggedInUser();
                    if (u != null && u.getId() == previousWinner.getId() && u instanceof Bidder b) {
                        b.setBalance(previousWinner.getBalance());
                        client.sendNotification(new Notification("BALANCE_UPDATE", b.getBalance()));
                        client.sendNotification(new Notification("OUTBID", String.format("Bạn đã bị vượt giá ở phiên #%d [%s]! Giá cao nhất hiện tại là $%,.2f.", id, item.getName(), amount)));
                        break;
                    }
                }
            }
        }

        // Gửi thông báo diễn biến phiên đấu giá cho Seller (nếu online)
        if (com.auction.server.AuctionServer.getInstance() != null) {
            for (ClientHandler client : com.auction.server.AuctionServer.getInstance().getConnectedClients()) {
                com.auction.model.user.User u = client.getLoggedInUser();
                if (u != null && u.getId() == item.getOwnerId()) {
                    if (bidHistory.size() == 1) {
                        client.sendNotification(new Notification("SELLER_FIRST_BID", String.format("Đã có người đặt giá đầu tiên cho sản phẩm [%s] của bạn: $%,.2f.", item.getName(), amount)));
                    }
                    if (bidHistory.size() % 5 == 0) {
                        client.sendNotification(new Notification("SELLER_BID_SURGE", String.format("Sản phẩm [%s] của bạn đang thu hút sự quan tâm với %d lượt đặt giá!", item.getName(), bidHistory.size())));
                    }
                    if (amount >= item.getStartingPrice() * 2) {
                        client.sendNotification(new Notification("SELLER_PRICE_MILESTONE", String.format("Sản phẩm [%s] của bạn đã vượt mốc giá gấp đôi giá khởi điểm: $%,.2f!", item.getName(), amount)));
                    }
                    break;
                }
            }
        }

        // Broadcast BID_UPDATE với BidUpdateData cho tất cả clients online
        if (com.auction.server.AuctionServer.getInstance() != null) {
            com.auction.server.AuctionServer.getInstance().broadcast(new Notification("BID_UPDATE",
                new com.auction.network.BidUpdateData(id, currentHighestBid, bidder.getUsername(), bidHistory.size())));
        }
    }

    private void readObject(java.io.ObjectInputStream ois) throws java.io.IOException, ClassNotFoundException {
        ois.defaultReadObject();
        if (observers == null) {
            observers = new CopyOnWriteArrayList<>();
        }
    }

    public void restoreStatus(AuctionStatus status) {
        this.status = status;
    }

    public void restoreHighestBid(double bid) {
        this.currentHighestBid = bid;
    }

    public void restoreCurrentWinner(Bidder winner) {
        this.currentWinner = winner;
    }

    public boolean hasObserver(Observer observer) {
        return observers.contains(observer);
    }
    //  Getters

    public int getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public Bidder getCurrentWinner() {
        return currentWinner;
    }

    public LocalDateTime getStartTime() {
        return startTime;

    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public double getMinBidStep() {
        return minBidStep;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public void setStatus(AuctionStatus status_) {
        status = status_;
    }

    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime ; }

    // Lấy số lượt xem của phiên đấu giá
    public int getViewCount() {
        return viewCount;
    }

    // Tăng số lượt xem của phiên đấu giá lên 1 (đồng bộ chống race-condition)
    public synchronized void incrementViewCount() {
        this.viewCount++;
    }

    // Giảm số lượt xem của phiên đấu giá đi 1 (đồng bộ chống race-condition)
    public synchronized void decrementViewCount() {
        if (this.viewCount > 0) {
            this.viewCount--;
        }
    }

    // Thiết lập/khôi phục số lượt xem của phiên đấu giá
    public void restoreViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    // bidder đăng kí auto-bid cho phiên này
    public synchronized void registerAutoBid(AutoBidConfig config){
        autoBidConfigs.put(config.getBidderId() , config);
        String msg = String.format("Auto-Bid: %s đặt auto-bid tối đa $%,.0f (bước +$%,.0f)",
                config.getBidderUsername() , config.getMaxBid() , config.getIncrement());
        notifyObservers(msg);

        // Kích hoạt Auto-Bid ngay lập tức nếu bidder vừa đăng ký không phải là winner hiện tại
        Bidder bidder = null;
        for (com.auction.pattern.observer.Observer obs : observers) {
            if (obs instanceof Bidder b && b.getId() == config.getBidderId()) {
                bidder = b;
                break;
            } else if (obs instanceof ClientHandler handler) {
                com.auction.model.user.User clientUser = handler.getLoggedInUser();
                if (clientUser != null && clientUser.getId() == config.getBidderId() && clientUser instanceof Bidder b) {
                    bidder = b;
                    break;
                }
            }
        }
        if (bidder == null) {
            com.auction.model.user.User u = UserManager.getInstance().findUserById(config.getBidderId());
            if (u instanceof Bidder b) {
                bidder = b;
            }
        }
        if (bidder != null) {
            triggerAutoBid(bidder);
        }
    }

    // hủy auto-bid
    public synchronized void cancelAutoBid(int bidderId){
        autoBidConfigs.remove(bidderId);
    }

    /** Kích hoạt auto-bid sau khi có người đặt giá thủ công.
     *  Tìm config auto-bid hợp lệ nhất (maxBid cao nhất) khác người vừa thắng,
     *  nếu có thì tự động đặt giá thêm 1 bước.
     */

    private void triggerAutoBid(Bidder justBidder) {
        // Vòng lặp đặt giá tự động ngay lập tức đối với các bên khác
        while (true) {
            AutoBidConfig best = null;
            double bestNextBid = 0;
            for (AutoBidConfig cfg : autoBidConfigs.values()) {
                if (currentWinner != null && cfg.getBidderId() == currentWinner.getId()) continue;
                double nextBid = currentHighestBid + cfg.getIncrement();
                if (nextBid > cfg.getMaxBid()) continue;
                if (best == null || cfg.getMaxBid() > best.getMaxBid()) {
                    best = cfg;
                    bestNextBid = nextBid;
                }
            }
            if (best == null) break;

            // Tìm đối tượng Bidder tương ứng
            Bidder bidder = null;
            for (com.auction.pattern.observer.Observer obs : observers) {
                if (obs instanceof Bidder b && b.getId() == best.getBidderId()) {
                    bidder = b;
                    break;
                } else if (obs instanceof ClientHandler handler) {
                    com.auction.model.user.User clientUser = handler.getLoggedInUser();
                    if (clientUser != null && clientUser.getId() == best.getBidderId() && clientUser instanceof Bidder b) {
                        bidder = b;
                        break;
                    }
                }
            }

            if (bidder == null) {
                com.auction.model.user.User u = UserManager.getInstance().findUserById(best.getBidderId());
                if (u instanceof Bidder b) {
                    bidder = b;
                }
            }

            if (bidder == null) break;
            Bidder activeBidder = bidder;

            try {
                placeBidInternal(activeBidder, bestNextBid, true);
            } catch (Exception e) {
                System.err.println("[AutoBid] Lỗi khi xử lý Auto-Bid: " + e.getMessage());
                break;
            }
        }
    }
    public Map<Integer , AutoBidConfig> getAutoBidConfigs(){
        return autoBidConfigs ;
    }

    /**
     * Anti-sniping: kiểm tra xem bid vừa đặt có trong snipe window không.
     * Nếu có → gia hạn endTime, thông báo tất cả observer.
     * Gọi từ placeBid() sau khi bid thành công.
     */

    private void checkAndExtend() {
        if (extensionCount >= MAX_EXTENSIONS) return;
        long secondsLeft = java.time.Duration.between(LocalDateTime.now(), endTime).toSeconds();
        if (secondsLeft > 0 && secondsLeft <= SNIPE_WINDOW_SECONDS) {
            endTime = endTime.plusSeconds(EXTENSION_SECONDS);
            extensionCount++;
            DataManager.getInstance().updateAuctionEndTime(id, endTime);
            String msg = String.format(
                    "Anti-Snipe! Phiên #%d được gia hạn thêm %ds (lần %d/%d) — kết thúc lúc %s",
                    id, EXTENSION_SECONDS, extensionCount, MAX_EXTENSIONS,
                    endTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            System.out.println(msg);
            notifyObservers(msg);
            notifyObservers("TIME_EXTENDED:" + endTime.toString()); // client parse để cập nhật đồng hồ

        }
    }

    public int getExtensionCount() {return extensionCount ;}
}