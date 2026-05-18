package com.auction.data;

import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.user.User;

import java.time.LocalDateTime;
import java.util.List;

public class DataManager {

    private static DataManager instance;

    private final UserRepository           userRepo;
    private final ItemRepository           itemRepo;
    private final AuctionRepository        auctionRepo;
    private final BidTransactionRepository bidRepo;

    private DataManager() {
        this.userRepo    = new UserRepository();
        this.itemRepo    = new ItemRepository();
        this.auctionRepo = new AuctionRepository();
        this.bidRepo     = new BidTransactionRepository();
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) instance = new DataManager();
        return instance;
    }

    // User

    public User authenticate(String username, String password) {
        return userRepo.authenticate(username, password);
    }

    public boolean registerUser(String username, String password, String email, String role) {
        return userRepo.register(username, password, email, role);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    // Item

    public int addItem(Item item) {
        return itemRepo.add(item);
    }

    public List<Item> getItemsBySeller(int sellerId) {
        return itemRepo.findBySeller(sellerId);
    }

    public List<Item> getAllItems() {
        return itemRepo.findAll();
    }

    // Auction

    public int createAuction(int itemId, LocalDateTime endTime, double minBidStep) {
        return auctionRepo.create(itemId, endTime, minBidStep);
    }

    public boolean updateAuctionBid(int auctionId, int bidderId, double amount) {
        return auctionRepo.updateBid(auctionId, bidderId, amount);
    }

    public boolean startAuction(int auctionId) {
        return auctionRepo.start(auctionId);
    }

    public boolean finishAuction(int auctionId) {
        return auctionRepo.finish(auctionId);
    }

    // BidTransaction

    public void saveBidTransaction(int auctionId, int bidderId, String bidderName, double amount) {
        bidRepo.save(auctionId, bidderId, bidderName, amount);
    }

    public List<BidTransaction> getBidHistory(int auctionId) {
        return bidRepo.findByAuction(auctionId);
    }
}