package com.auction.manager;


import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class AuctionManager {
    private static volatile AuctionManager instance ;
    // FIX: đổi ArrayList → ConcurrentHashMap để an toàn khi nhiều thread truy cập
    private final ConcurrentHashMap<Integer, Auction> auctions = new ConcurrentHashMap<>();   // Danh sách các phiên đấu
    private final AtomicInteger AuctionCounter = new AtomicInteger(1);              // tránh Rase condition khi counter++
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
    public Auction createAuction(Item item, LocalDateTime endTime, double minBidStep) { // minStep là bước giá tối thiểu nhé
        if (item.getStatus() != ItemStatus.AVAILABLE) {
            System.out.println("Sản phẩm không ở trạng thái AVAILABLE: " + item.getName());
            return null;
        }
        int id = AuctionCounter.getAndIncrement();
        Auction auction = new Auction(id, item, endTime, minBidStep);
        item.setStatus(ItemStatus.IN_AUCTION);
        auctions.put(id, auction);
        System.out.printf("Phiên #%d tạo cho [%s] — kết thúc: %s%n", id, item.getName(), endTime);
        return auction;
    }

    public List<Auction> getAuctions() {
        return auctions.values().stream().collect(Collectors.toList());
    }

    public List<Auction> getByStatus(AuctionStatus status) {
        return auctions.values().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    public Auction findAuctionById(int id) {
        Auction a = auctions.get(id);
        if (a == null) System.out.println("Phiên không tồn tại: " + id);
        return a;
    }
}
