package com.auction.manager;

import com.auction.data.AuctionDAO;
import com.auction.data.ItemDAO;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;

import java.io.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AuctionManager implements Serializable {
    private static volatile AuctionManager instance ;
    // FIX: đổi ArrayList → ConcurrentHashMap để an toàn khi nhiều thread truy cập
    private final ConcurrentHashMap<Integer, Auction> auctions = new ConcurrentHashMap<>();   // Danh sách các phiên đấu
    private AtomicInteger auctionCounter = new AtomicInteger(1);// tránh Race condition khi counter++
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final ItemDAO itemDAO = new ItemDAO();

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
        int id = auctionCounter.getAndIncrement();
        Auction auction = new Auction(id, item, endTime, minBidStep);
        item.setStatus(ItemStatus.IN_AUCTION);
        itemDAO.updateStatus(item.getId() , "IN_AUCTION");
        int realId = auctionDAO.save(auction); // lưu vào MySQL
        if (realId == -1 ) return null ;

        Auction saved = new Auction(realId , item , endTime , minBidStep);
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
    // gọi sau mỗi lần placeBid - lưu vào MySQL
    public void onBidPlaced(Auction auction , BidTransaction tx){
        auctionDAO.update(auction);
        auctionDAO.saveBidTransaction(auction.getId() , tx);
    }
    //Gọi khi phiên kết thúc
    public void onAuctionFinished(Auction auction){
        auctionDAO.update(auction);
        if (auction.getStatus() == AuctionStatus.FINISHED ||
            auction.getStatus() == AuctionStatus.CANCELED) {
            itemDAO.updateStatus(auction.getItem().getId() , "SOLD");
        }
    }

    public void loadFromDB(){
        List<Auction> list = auctionDAO.findAll(itemDAO);
        auctions.clear();
        list.forEach(a -> auctions.put(a.getId(),a));
        if (!list.isEmpty()){
            int maxId = list.stream().mapToInt(Auction::getId).max().getAsInt();
            auctionCounter.set(maxId + 1);
        }
        System.out.println("Đã load " + auctions.size() + " auctions từ MySQL");
    }
    public void saveAuction(Auction auction){
        auctionDAO.update(auction);
    }

}
