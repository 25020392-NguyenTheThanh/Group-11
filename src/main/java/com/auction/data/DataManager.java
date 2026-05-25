package com.auction.data;

import com.auction.model.auction.Auction;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.user.User;

import java.time.LocalDateTime;
import java.util.List;

public class DataManager {

    private static DataManager instance;

    private final UserRepository userRepo;
    private final ItemRepository itemRepo;
    private final AuctionRepository auctionRepo;
    private final BidTransactionRepository bidRepo;

    private DataManager() {
        this.userRepo = new UserRepository();
        this.itemRepo = new ItemRepository();
        this.auctionRepo = new AuctionRepository();
        this.bidRepo = new BidTransactionRepository();
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) instance = new DataManager();
        return instance;
    }

    public boolean updateAuctionEndTime(int auctionId, java.time.LocalDateTime newEndTime) {
        return auctionRepo.updateEndTime(auctionId, newEndTime);
    }

    // User
    public User authenticate(String username, String password) {
        return userRepo.authenticate(username, password);
    }

    /** Trả "ok" | "USERNAME_EXISTS" | "EMAIL_EXISTS" | mô tả lỗi */
    public String registerUser(String username, String password, String email, String role) {
        return userRepo.register(username, password, email, role);
    }

    public List<User> getAllUsers() { return userRepo.findAll(); }
    public boolean deleteItem(int id) {
        return itemRepo.delete(id);
    }

    public boolean updateItem(Item item) {
        return itemRepo.update(item);
    }

    public boolean updateItemStatus(int itemId, com.auction.model.item.ItemStatus status) {
        return itemRepo.updateStatus(itemId, status);
    }

    // Item
    public int addItem(Item item) { return itemRepo.add(item); }
    public List<Item> getItemsBySeller(int sellerId) { return itemRepo.findBySeller(sellerId); }
    public List<Item> getAllItems() { return itemRepo.findAll(); }

    // Auction
    public int createAuction(int itemId,LocalDateTime startTime, LocalDateTime endTime, double minBidStep) {
        return auctionRepo.create(itemId,startTime, endTime, minBidStep);
    }

    public boolean updateAuctionBid(int auctionId, int bidderId, double amount) {
        return auctionRepo.updateBid(auctionId, bidderId, amount);
    }

    public boolean startAuction(int auctionId) {
        return auctionRepo.start(auctionId);
    }

    /** Kết thúc phiên — truyền đúng finalStatus ("FINISHED" hoặc "CANCELED"). */
    public boolean finishAuction(int auctionId, String finalStatus) {
        return auctionRepo.finish(auctionId, finalStatus);
    }

    /** @deprecated dùng finishAuction(id, status). */
    @Deprecated
    public boolean finishAuction(int auctionId) {
        return auctionRepo.finish(auctionId);
    }

    public void saveBidderWon(int bidderId, int auctionId) {
        auctionRepo.saveBidderWon(bidderId, auctionId);
    }

    public List<Auction> getAllAuctions() {
        return auctionRepo.findAll();
    }

    // BidTransaction

    public void saveBidTransaction(int auctionId, int bidderId, String bidderName, double amount, String bidType) {
        bidRepo.save(auctionId, bidderId, bidderName, amount, bidType);
    }

    public List<BidTransaction> getBidHistory(int auctionId) {
        return bidRepo.findByAuction(auctionId);
    }

    public boolean updateSellerRevenue(int sellerId, double amount) {
        return userRepo.addSellerRevenue(sellerId, amount);
    }

    public boolean updateBidderBalance(int bidderId, double newBalance) {
        return userRepo.updateBidderBalance(bidderId, newBalance);
    }

    public boolean addToWatchlist(int bidderId, int auctionId) {
        return userRepo.addToWatchlist(bidderId, auctionId);
    }

    public boolean removeFromWatchlist(int bidderId, int auctionId) {
        return userRepo.removeFromWatchlist(bidderId, auctionId);
    }

    public boolean markBidderToppedUp(int bidderId) {
        return userRepo.markBidderToppedUp(bidderId);
    }

    public boolean updatePassword(int userId, String newPassword) {
        return userRepo.updatePassword(userId, newPassword);
    }

    /**
     * Thanh toán phiên đấu giá: cập nhật auction → PAID, item → SOLD trong DB.
     */
    public boolean payAuction(int auctionId) {
        return auctionRepo.payAuction(auctionId);
    }

}