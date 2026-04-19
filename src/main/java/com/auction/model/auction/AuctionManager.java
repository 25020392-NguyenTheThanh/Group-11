package com.auction.model.auction;


import com.auction.model.user.User;

import java.util.ArrayList;

public class AuctionManager {
    private static volatile AuctionManager instance ;
    private static ArrayList<User> users = new ArrayList<>() ; // Danh sách người dùng
    private static ArrayList<Auction> auctions = new ArrayList<>(); // Danh sách các phiên đấu

    private AuctionManager(){}

    public static AuctionManager getInstance(){
        if (instance == null){
            synchronized (AuctionManager.class){
                if (instance == null){
                    instance = new AuctionManager();
                }
            }
        }
        return instance ;
    }

    public void addAuction(Auction auction) { auctions.add(auction); }
    public ArrayList<Auction> getAuctions() { return auctions; }
    public void addUser(User user) { users.add(user); }
    public ArrayList<User> getUsers() { return users; }

    // tìm user theo username
    public User findByUsername(String username) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }
}
