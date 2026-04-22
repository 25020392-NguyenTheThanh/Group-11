package com.auction.model.user;

public class Seller extends User{
    public Seller(int id, String username, String password, String email) {
        super(id, username, password, email);
    }
    @Override
    public String getRole(){ return "SELLER" ; }
}
