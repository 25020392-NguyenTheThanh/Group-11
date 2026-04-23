package com.auction.model.user;

import com.auction.pattern.observer.Observer;

// Bidder đóng vai trò là Observer sẽ nhâ thông báo khi có bid mới

public class Bidder extends User implements Observer {
    public Bidder(int id, String username, String password, String email) {
        super(id, username, password, email);
    }

    @Override
    public String getRole() { return "BIDDER"; }

    @Override
    public void send(String message) {
        System.out.println("[Notification for " + getUsername() + "]: " + message);
    }
}
