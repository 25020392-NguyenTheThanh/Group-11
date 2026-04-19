package com.auction.model.user;

public class Bidder extends User {
    public Bidder(int id, String username, String password, String email) {
        super(id, username, password, email);
    }

    @Override
    public String getRole() { return "BIDDER"; }
}
