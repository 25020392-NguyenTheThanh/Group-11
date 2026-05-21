package com.auction.model.auction;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.pattern.observer.Observer;
import com.auction.pattern.observer.Subject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Auction đóng vai trò là Subject — notify tất cả observer khi có thay đổi
// sửa thêm một số chỗ tránh race condition!!

public class Auction implements Subject {
    private int id;
    private Item item;
    private double currentHighestBid;
    private Bidder currentWinner;
    private AuctionStatus status;
    private LocalDateTime endTime;
    private List<BidTransaction> bidHistory;
    private transient CopyOnWriteArrayList<Observer> observers; // không lưu vào file
    private final double minBidStep; // bước giá tối thiểu

    public Auction(int id, Item item, LocalDateTime endTime, double minBidStep) {
        this.id = id;
        this.item = item;
        this.currentHighestBid = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        this.endTime = endTime;
        this.minBidStep = minBidStep; // bước giá tối thiểu
        this.bidHistory = new ArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
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
                "▶ Phiên #%d [%s] bắt đầu! Giá khởi điểm: %,.0f₫ | Bước giá tối thiểu: %,.0f₫",
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
                    "■ Phiên #%d [%s] kết thúc — Người thắng: %s với giá %,.0f₫",
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
                "💰 Phiên #%d [%s] — %s đã thanh toán thành công %,.0f₫",
                id, item.getName(), currentWinner.getUsername(), currentHighestBid);
        System.out.println(msg);
        notifyObservers(msg);
    }

    //  Đặt giá — synchronized chống race condition

    public synchronized void placeBid(Bidder bidder, double amount)
            throws InvalidBidException, AuctionClosedException {

        // Tính xác thực
        if (bidder == null || !bidder.isAuthenticated()) {
            throw new AuthenticationException("Người dùng chưa được xác thực");
        }

        // Kiểm tra số tiền
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }

        // Kiểm tra thời gian thực
        if (LocalDateTime.now().isAfter(endTime)) {
            finish();
            throw new AuctionClosedException("Phiên #" + id + " đã hết giờ.");
        }

        // Kiểm tra trạng thái phiên đấu giá
        if (status != AuctionStatus.RUNNING)
            throw new AuctionClosedException(String.format(
                    "Phiên #%d không ở trạng thái RUNNING (hiện: %s)", id, status));

        // Kiê tra số dư
        if (bidder.getBalance() < amount)
            throw new InvalidBidException(String.format(
                    "Số dư không đủ — số dư hiện tại: %,.0f₫, giá đặt: %,.0f₫",
                    bidder.getBalance(), amount));

        double minAccepted = currentHighestBid + minBidStep;

        if (amount < minAccepted)
            throw new InvalidBidException(String.format(
                    "Giá đặt phải >= %,.0f₫  (giá cao nhất %,.0f₫ + bước tối thiểu %,.0f₫)",
                    minAccepted, currentHighestBid, minBidStep));

        bidder.deduct(amount);
        // FIX: lưu vào lịch sử — phiên bản gốc bỏ sót hoàn toàn
        BidTransaction tx = new BidTransaction(bidder.getId(), bidder.getUsername(), amount);
        bidHistory.add(tx);

        currentHighestBid = amount;
        currentWinner = bidder;

        String msg = String.format(
                "🔔 Phiên "+id+" — "+bidder.getUsername()+" vừa đặt "+amount+"₫ | Giá cao nhất hiện tại: "+ currentHighestBid);
        System.out.println(msg);
        notifyObservers(msg);
    }

    private void readObject(java.io.ObjectInputStream ois) throws java.io.IOException , ClassNotFoundException{
        ois.defaultReadObject();
        if (observers == null){
            observers = new CopyOnWriteArrayList<>();
        }
    }
    public void restoreStatus(AuctionStatus status){
        this.status = status ;
    }
    public void restoreHighestBid(double bid){
        this.currentHighestBid = bid ;
    }

    public boolean hasObserver(Observer observer){
        return observers.contains(observer);
    }
    //  Getters

    public int                  getId()                { return id; }
    public Item                 getItem()              { return item; }
    public AuctionStatus        getStatus()            { return status; }
    public double               getCurrentHighestBid() { return currentHighestBid; }
    public Bidder               getCurrentWinner()     { return currentWinner; }
    public LocalDateTime        getEndTime()           { return endTime; }
    public List<BidTransaction> getBidHistory()        { return bidHistory; }
    public double               getMinBidStep()        { return minBidStep; }
    public boolean              isExpired()            { return LocalDateTime.now().isAfter(endTime); }
    public void                 setStatus(AuctionStatus status_) {status = status_;}
}