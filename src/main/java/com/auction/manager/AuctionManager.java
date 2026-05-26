package com.auction.manager;

import com.auction.data.DataManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.pattern.observer.Observer;
import com.auction.server.ClientHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// AuctionManager — quản lý phiên đấu giá + đồng bộ MySQL.
public class AuctionManager {

    private static volatile AuctionManager instance;

    // Bộ nhớ runtime — key = auctionId
    private final ConcurrentHashMap<Integer, Auction> auctions = new ConcurrentHashMap<>();
    private final AtomicInteger auctionCounter = new AtomicInteger(1);

    private AuctionManager() {
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Tạo phiên đấu giá mới, lưu vào MySQL, giữ trong RAM.
     * return Auction vừa tạo, hoặc null nếu item không ở trạng thái AVAILABLE.
     */
    public Auction createAuction(Item item, LocalDateTime startTime, LocalDateTime endTime, double minBidStep) {
        if (item.getStatus() != ItemStatus.AVAILABLE) {
            System.out.println("Sản phẩm không ở trạng thái AVAILABLE: " + item.getName());
            return null;
        }

        // Lưu vào MySQL — lấy id thật từ DB
        int dbId = DataManager.getInstance().createAuction(item.getId(), startTime, endTime, minBidStep);
        if (dbId == -1) {
            System.out.println("Lỗi tạo phiên trong DB cho item: " + item.getName());
            return null;
        }

        item.setStatus(ItemStatus.IN_AUCTION);

        Auction auction = new Auction(dbId, item, startTime, endTime, minBidStep);
        auctions.put(dbId, auction);

        System.out.printf("Phiên #%d tạo cho [%s] — kết thúc: %s%n",
                dbId, item.getName(), endTime);
        return auction;
    }

    // Ghi nhận một lần đặt giá: cập nhật + MySQL + lưu lịch sử bid.
    public void recordBid(int auctionId, int bidderId, String bidderName, double amount, String bidType) {
        if (DataManager.isTestMode()) return;
        // Cập nhật bảng auctions (current_highest_bid, current_winner_id)
        DataManager.getInstance().updateAuctionBid(auctionId, bidderId, amount);
        // Ghi lịch sử vào bid_transactions
        DataManager.getInstance().saveBidTransaction(auctionId, bidderId, bidderName, amount, bidType);
    }

    // Kết thúc phiên: cập nhật đúng trạng thái (FINISHED hoặc CANCELED) trong MySQL.
    public void finishAuction(int auctionId) {
        Auction auction = auctions.get(auctionId);
        String finalStatus = (auction != null)
                ? auction.getStatus().name()   // lấy trạng thái thực tế từ RAM
                : "FINISHED";
        DataManager.getInstance().finishAuction(auctionId, finalStatus);
    }

    /**
     * Xác nhận thanh toán phiên đấu giá (FINISHED → PAID).
     * Cập nhật DB và ghi nhận bidder thắng.
     */
    public boolean payAuction(int auctionId) {
        return DataManager.getInstance().payAuction(auctionId);
    }

    public List<Auction> getAuctions() {
        return auctions.values().stream().collect(Collectors.toList());
    }

    public void removeAuctionFromCache(int auctionId) {
        auctions.remove(auctionId);
    }

    public List<Auction> getByStatus(AuctionStatus status) {
        return auctions.values().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    public Auction findAuctionById(int id) {
        Auction a = auctions.get(id);
        if (a == null) System.out.println("Phiên không tồn tại trong RAM: " + id);
        return a;
    }

    public void loadAuctionsFromDatabase() {
        List<Auction> dbAuctions = DataManager.getInstance().getAllAuctions();
        LocalDateTime now = LocalDateTime.now();
        for (Auction a : dbAuctions) {
            List<BidTransaction> history = DataManager.getInstance().getBidHistory(a.getId());
            a.getBidHistory().clear();
            a.getBidHistory().addAll(history);

            // Khôi phục giá cao nhất và người thắng cuộc từ lịch sử thực tế nếu có lịch sử đặt giá
            if (!history.isEmpty()) {
                BidTransaction highestBidTx = history.get(history.size() - 1);
                a.restoreHighestBid(highestBidTx.getAmount());
                User winner = UserManager.getInstance().findUserById(highestBidTx.getBidderId());
                if (winner instanceof Bidder b) {
                    a.restoreCurrentWinner(b);
                }
                // Đồng bộ ngược lại bảng auctions để dọn sạch dữ liệu cũ/lệch trong DB
                DataManager.getInstance().updateAuctionBid(a.getId(), highestBidTx.getBidderId(), highestBidTx.getAmount());
            }

            // Tự động kích hoạt nếu phiên OPEN đã đến giờ bắt đầu lúc khởi động server
            if (a.getStatus() == AuctionStatus.OPEN && (a.getStartTime() == null || !a.getStartTime().isAfter(now))) {
                try {
                    a.start();
                    DataManager.getInstance().startAuction(a.getId());
                    System.out.println("Tự động kích hoạt phiên #" + a.getId() + " lúc khởi chạy Server.");
                } catch (Exception e) {
                    System.err.println("Lỗi khi tự động kích hoạt phiên #" + a.getId() + " lúc khởi chạy Server: " + e.getMessage());
                }
            }

            auctions.put(a.getId(), a);
        }
        int maxId = dbAuctions.stream().mapToInt(Auction::getId).max().orElse(0);
        auctionCounter.set(maxId + 1);
        System.out.println("Loaded " + dbAuctions.size() + " auctions from database.");
    }

    public void removeObserverFromAllAuctions(Observer observer) {
        for (Auction a : auctions.values()) {
            if (a.hasObserver(observer)) {
                boolean hasOtherConnection = false;
                if (observer instanceof ClientHandler ch) {
                    User u = ch.getLoggedInUser();
                    if (u != null) {
                        for (Observer obs : a.getObservers()) {
                            if (obs != observer && obs instanceof ClientHandler otherCh) {
                                User otherUser = otherCh.getLoggedInUser();
                                if (otherUser != null && otherUser.getId() == u.getId()) {
                                    hasOtherConnection = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                a.removeObserver(observer);
                if (!hasOtherConnection) {
                    a.decrementViewCount();
                }
                a.notifyObservers("VIEW_UPDATE");
            }
        }
    }

    @Deprecated
    public void loadFromDisk() {
        System.out.println("[AuctionManager] loadFromDisk() đã bị bỏ — dữ liệu lấy từ MySQL.");
    }

    @Deprecated
    public void saveToDisk() {
        System.out.println("[AuctionManager] saveToDisk() đã bị bỏ — dữ liệu lưu vào MySQL.");
    }
}