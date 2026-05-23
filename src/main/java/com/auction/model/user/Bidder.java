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
    private final BidderProfile profile ; // id phiên đấu giá đã từng thắng
    private boolean hasToppedUp; // trạng thái đã nạp tiền (chỉ được nạp 1 lần)

    public Bidder(int id, String username, String password, String email, double balance, boolean hasToppedUp) {
        super(id, username, password, email);
        this.balance = balance;
        this.hasToppedUp = hasToppedUp;
        this.profile = new BidderProfile(id);
    }
    public double getBalance() {
        return balance;
    }

    public void topUp(double amount){ // nạp tiền
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải là số dương");
        }
        this.balance += amount;
    }
    public synchronized void deduct(double amount) throws InvalidBidException{ // trừ tiền
        if (amount > balance) {
            throw new InvalidBidException("Số dư không đủ");
        }
        balance -= amount;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean hasToppedUp() {
        return hasToppedUp;
    }

    public void setHasToppedUp(boolean hasToppedUp) {
        this.hasToppedUp = hasToppedUp;
    }

    public BidderProfile getProfile() { return profile; }

    @Override
    public String getRole() { return "BIDDER"; }

    @Override
    public void send(String message) {
        System.out.println("[Notification for " + getUsername() + "]: " + message);
    }


}
