package com.auction.model.auction;


import com.auction.model.item.Item;
import com.auction.model.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    private static volatile AuctionManager instance ;
    private static ArrayList<Auction> auctions = new ArrayList<>(); // Danh sách các phiên đấu

    private int AuctionCounter = 1 ;
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

    // tạo 1 phiên mới
    public Auction createAuction(Item item, LocalDateTime endTime) {
        int id = AuctionCounter++;
        Auction auction = new Auction(id, item, endTime);
        auctions.add(auction);
        return auction;
    }

    public List<Auction> getAuctions() { return auctions; }

    // tìm phiên theo id
    public Auction findAuctionById(int id) {
        for (Auction a : auctions) {
            if (a.getId() == id) return a;
        }
        System.out.println("Phiên không tồn tại");
        return null;
    }

}
