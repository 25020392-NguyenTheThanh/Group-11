package com.auction.model.user;

public class Admin extends User {
    public Admin(int id, String username, String password, String email) {
        super(id, username, password, email);
    }

    @Override
    public String getRole() { return "ADMIN"; }
}