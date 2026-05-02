package com.auction.model.user;

import com.auction.exception.InvalidBidException;
import com.auction.pattern.observer.Observer;

import java.util.ArrayList;
import java.util.List;

// Bidder đóng vai trò là Observer sẽ nhân thông báo khi có bid mới
// cập nhật thêm thông tin người đấu giá bao gồm số dư, lịch sử đấu giá
// thêm một số phương thức để cập nhật số dư, hay số lần đấu giá thành công
public class Bidder extends User implements Observer {
    private double balance; // số dư của ví
    private final List<Integer> wonAuctions; // id phiên đấu giá đã từng thắng

    public Bidder(int id, String username, String password, String email,double balance) {
        super(id, username, password, email);
        this.balance = balance;
        this.wonAuctions = new ArrayList<>();
    }
    public double getBalance() {
        return balance;
    }

    public void topUp(double amount){
        if (amount <= 0) {
            throw new IllegalArgumentException("Top up must be positive");
        }
        this.balance += amount;
    }
    public void deduct(double amount){
        if (amount > balance) {
            throw new InvalidBidException("Insufficient balance");
        }
        balance -= amount;
    }
    public void addWonAuction(int AuctionId){
        wonAuctions.add(AuctionId);
    }
    public List<Integer> getWonAuctions() {return wonAuctions;}

    @Override
    public String getRole() { return "BIDDER"; }

    @Override
    public void send(String message) {
        System.out.println("[Notification for " + getUsername() + "]: " + message);
    }
}
