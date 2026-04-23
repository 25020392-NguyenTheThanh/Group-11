package com.auction.model.auction;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.pattern.observer.Observer;
import com.auction.pattern.observer.Subject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//Auction đóng vai trò là Subject sẽ notify tất cả bidder khi có thay đổi

public class Auction implements Subject {
    private int id ; // id của phiên đấu giá
    private Item item ; // sản phẩm đang đấu giá
    private double currentHighestBid ; // giá cao nhất hiện tại
    private Bidder currentWinner ; // người đang dẫn đầu
    private AuctionStatus status ;
    private LocalDateTime endTime ;
    private List<BidTransaction> bidHistory ; // danh sách các lần đặt giá
    private List<Observer> observers; // danh sách observer

    public Auction(int id , Item item , LocalDateTime endTime){
        this.id = id ;
        this.item = item ;
        this.currentHighestBid = item.getStartingPrice() ; // giá khởi điểm
        this.status = AuctionStatus.OPEN ;
        this.endTime = endTime ;
        this.bidHistory = new ArrayList<>();
        this.observers = new ArrayList<>();
    }
    // xử lý các lần đặt giá
    public synchronized void placeBid(Bidder bidder , double amount) throws InvalidBidException , AuctionClosedException {

        // kiểm tra phiên có đang chạy hay không
        if (status != AuctionStatus.RUNNING){
            throw new AuctionClosedException("Phiên đang không tồn tại");
        }

        // kiểm tra giá
        if (amount <= currentHighestBid){
            throw new InvalidBidException("Giá phải cao hơn " + currentHighestBid);
        }

        // cập nhật giá, người chơi thắng vòng đấu giá
        currentHighestBid = amount ;
        currentWinner = bidder ;

        // lưu lịch sử giao dịch
        bidHistory.add((new BidTransaction(bidder , amount)));

        // tự động đăng ký bidder nếu chưa có
        if (!observers.contains(bidder)) {  // Kiểm tra Observer có bidder chưa
            observers.add(bidder);
        }
        // thông báo tất cả observer
        notifyObservers("New bid: " + amount + " by " + bidder.getUsername());
    }

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

    public void start(){
        this.status = AuctionStatus.RUNNING;
        notifyObservers("Auction started!");
    }
    public void finish() {
        this.status = AuctionStatus.FINISHED;
        String message;
        // Thông báo kết quả
        if (currentWinner != null) {
            message = "Auction finished! Winner: " + currentWinner.getUsername();
        } else {
            message = "Auction finished! No winner";
        }
    }


    public int getId() {
        return id;
    }
    public Item getItem() {
        return item;
    }
    public double getCurrentHighestBid() {
        return currentHighestBid;
    }
    public Bidder getCurrentWinner() {
        return currentWinner;
    }
    public AuctionStatus getStatus() {
        return status;
    }
    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

}
