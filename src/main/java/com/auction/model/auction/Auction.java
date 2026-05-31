package com.auction.model.auction;

import com.auction.data.DataManager;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.BidUpdateData;
import com.auction.network.Notification;
import com.auction.pattern.observer.Observer;
import com.auction.pattern.observer.Subject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.auction.server.AuctionServer;
import com.auction.server.ClientHandler;
import com.auction.manager.UserManager;
import com.auction.manager.AuctionManager;


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
    private List<Integer> uniqueViewerIds; // danh sách mã người dùng đã xem
    private Map<Integer , AutoBidConfig> autoBidConfigs = new ConcurrentHashMap<>();
    private static final transient java.util.concurrent.ScheduledExecutorService autoBidExecutor = 
        java.util.concurrent.Executors.newScheduledThreadPool(4);

    private transient boolean isProcessingAutoBid = false;
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
        this.uniqueViewerIds = new ArrayList<>();
    }

    // Observer

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
        updateViewCountInternal();
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
        updateViewCountInternal();
    }

    private void updateViewCountInternal() {
        if (observers != null) {
            this.viewCount = (int) observers.stream()
                    .map(obs -> {
                        if (obs instanceof ClientHandler ch) {
                            return ch.getLoggedInUser();
                        } else if (obs instanceof User u) {
                            return u;
                        }
                        return null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .map(User::getId)
                    .distinct()
                    .count();
        }
    }

    @Override
    public void notifyObservers(String message) {
        for (Observer o : observers) {
            o.send(message);
        }
    }

    // Lifecycle

    public synchronized void start() {
        if (status != AuctionStatus.OPEN)
            throw new IllegalStateException("Chỉ có thể bắt đầu phiên ở trạng thái OPEN");

        status = AuctionStatus.RUNNING;
        if (item != null) {
            item.setStatus(ItemStatus.IN_AUCTION);
        }

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
            if (item != null) {
                item.setStatus(ItemStatus.UNSOLD);
            }
            msg = String.format(
                    "■ Phiên #%d [%s] kết thúc — Không có người đặt giá. Trạng thái: CANCELED.",
                    id, item.getName());
        } else {
            status = AuctionStatus.FINISHED;
            if (item != null) {
                item.setStatus(ItemStatus.SOLD);
            }
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
        if (!isAutoBid && (bidder == null || !bidder.isAuthenticated()))
            throw new AuthenticationException("Người dùng chưa được xác thực");

        new BidProcessor(this).execute(bidder, amount, isAutoBid);

        if (!isAutoBid) {
            if (DataManager.isTestMode()) {
                new AutoBidEngine(this).trigger(bidder);
            } else {
                autoBidExecutor.schedule(() -> new AutoBidEngine(this).trigger(bidder), 3, java.util.concurrent.TimeUnit.SECONDS);
            }
        }
    }

    // Auto-bid

    public synchronized void registerAutoBid(AutoBidConfig config) {
        autoBidConfigs.put(config.getBidderId(), config);
        String msg = String.format("Auto-Bid: %s đặt auto-bid tối đa $%,.0f (bước +$%,.0f)",
                config.getBidderUsername(), config.getMaxBid(), config.getIncrement());
        notifyObservers(msg);
        new AutoBidEngine(this).triggerForConfig(config);
    }

    public synchronized void cancelAutoBid(int bidderId) {
        autoBidConfigs.remove(bidderId);
    }

    // Serialization

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        if (observers == null) observers = new CopyOnWriteArrayList<>();
        if (uniqueViewerIds == null) uniqueViewerIds = new ArrayList<>();
    }

    public synchronized void recordViewer(int userId) {
        if (uniqueViewerIds == null) uniqueViewerIds = new ArrayList<>();
        if (!uniqueViewerIds.contains(userId)) {
            uniqueViewerIds.add(userId);
            this.viewCount = uniqueViewerIds.size();
        }
    }

    // Restore helpers

    public void restoreStatus(AuctionStatus status)    { this.status = status; }
    public void restoreHighestBid(double bid)           { this.currentHighestBid = bid; }
    public void restoreCurrentWinner(Bidder winner)     { this.currentWinner = winner; }
    public void restoreViewCount(int viewCount)         { this.viewCount = viewCount; }

    public boolean hasObserver(Observer observer)       { return observers.contains(observer); }
    public List<Observer> getObservers()                { return observers; }

    // Getters

    public int getId()                          { return id; }
    public Item getItem()                       { return item; }
    public void setItem(Item item)              { this.item = item; }
    public AuctionStatus getStatus()            { return status; }
    public void setStatus(AuctionStatus s)      { this.status = s; }
    public double getCurrentHighestBid()        { return currentHighestBid; }
    public Bidder getCurrentWinner()            { return currentWinner; }
    public LocalDateTime getStartTime()         { return startTime; }
    public LocalDateTime getEndTime()           { return endTime; }
    public void setEndTime(LocalDateTime t)     { this.endTime = t; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public double getMinBidStep()               { return minBidStep; }
    public Map<Integer, AutoBidConfig> getAutoBidConfigs() { return autoBidConfigs; }
    public int getExtensionCount()              { return extensionCount; }
    public boolean isExpired()                  { return LocalDateTime.now().isAfter(endTime); }

    public int getViewCount() {
        if (observers != null && !observers.isEmpty()) updateViewCountInternal();
        return this.viewCount;
    }

    public synchronized void incrementViewCount() {}
    public synchronized void decrementViewCount() {}

    // Package-private cho BidProcessor / AntiSnipeGuard
    void setCurrentHighestBid(double bid)   { this.currentHighestBid = bid; }
    void setCurrentWinner(Bidder winner)    { this.currentWinner = winner; }
    void incrementExtensionCount()          { this.extensionCount++; }
    java.util.concurrent.ScheduledExecutorService getAutoBidExecutor() { return autoBidExecutor; }
}