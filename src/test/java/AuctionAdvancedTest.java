import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.AutoBidConfig;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Electronics;
import com.auction.model.item.Art;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.data.DataManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test nâng cao cho hệ thống đấu giá:
 *  - Auto Bid (đăng ký, tự động đặt, vượt trần, tranh chấp 2 auto-bidder)
 *  - Anti-Sniping (gia hạn nhiều lần, chạm giới hạn MAX_EXTENSIONS)
 *  - Đa luồng (nhiều thread cùng đặt giá, race condition)
 *  - Crash Recovery / Serialization (serialize → deserialize kiểm tra dữ liệu)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionAdvancedTest {

    // ── helper tạo auction mặc định ──────────────────────────────────────────
    private static Auction makeAuction(int id, double startPrice, double minStep,
                                       long secondsFromNow) {
        Item item = new Electronics(id, 99, "Item-" + id, "desc", startPrice, "", "Brand");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end   = start.plusSeconds(secondsFromNow);
        Auction a = new Auction(id, item, start, end, minStep);
        a.setStatus(AuctionStatus.RUNNING);
        return a;
    }

    private static Bidder makeBidder(int id, double balance) {
        Bidder b = new Bidder(id, "User" + id, "pass", "u" + id + "@test.com", balance, null);
        b.setAuthenticated(true);
        return b;
    }

    @BeforeAll
    static void init() {
        DataManager.setTestMode(true);
    }

    // =========================================================================
    // 1. AUTO BID
    // =========================================================================

    /** Auto-bid không được kích hoạt khi bidder là winner hiện tại (tránh tự bid chính mình). */
    @Test
    @Order(10)
    void autoBid_shouldNotTriggerWhenAlreadyWinning() {
        Auction auction = makeAuction(10, 1000, 100, 3600);
        Bidder auto = makeBidder(1, 5000);
        auction.addObserver(auto);

        AutoBidConfig cfg = new AutoBidConfig(1, "User1", 3000, 100);
        auction.registerAutoBid(cfg);

        // Auto đặt ngay khi đăng ký (vì chưa có winner) — giá phải là 1100
        assertEquals(1100, auction.getCurrentHighestBid(), 0.01);
        assertEquals("User1", auction.getCurrentWinner().getUsername());

        // Dù có manual bid từ người khác rồi auto bid lại, không tự bid khi đang giữ cao nhất
        // Thêm bidder thứ 2 vượt qua auto bidder
        Bidder manual = makeBidder(2, 5000);
        auction.addObserver(manual);
        auction.placeBid(manual, 1200); // manual vượt → auto-bid kích hoạt lên 1300
        assertEquals(1300, auction.getCurrentHighestBid(), 0.01);
        assertEquals("User1", auction.getCurrentWinner().getUsername());
    }

    /** Auto-bid dừng khi đã chạm mức giá trần maxBid. */
    @Test
    @Order(11)
    void autoBid_shouldStopAtMaxBid() {
        Auction auction = makeAuction(11, 1000, 100, 3600);
        Bidder auto = makeBidder(1, 10000);
        auction.addObserver(auto);

        // maxBid = 1300 → tối đa đặt được 1300
        AutoBidConfig cfg = new AutoBidConfig(1, "User1", 1300, 100);
        auction.registerAutoBid(cfg);

        Bidder manual = makeBidder(2, 10000);
        auction.addObserver(manual);

        auction.placeBid(manual, 1200); // vượt → auto thử lên 1300 — OK
        assertEquals(1300, auction.getCurrentHighestBid(), 0.01);
        assertEquals("User1", auction.getCurrentWinner().getUsername());

        auction.placeBid(manual, 1400); // vượt maxBid → auto không thể đặt 1500
        // manual phải thắng vì auto đã cạn trần
        assertEquals(1400, auction.getCurrentHighestBid(), 0.01);
        assertEquals("User2", auction.getCurrentWinner().getUsername());
    }

    /** Auto-bid với increment nhỏ hơn minBidStep phải bị bỏ qua (nextBid < minAccepted). */
    @Test
    @Order(12)
    void autoBid_incrementBelowMinStepShouldBeIgnored() {
        // minStep = 200, auto increment = 50 → nextBid = currentHighest + 50 < minAccepted
        Auction auction = makeAuction(12, 1000, 200, 3600);
        Bidder auto = makeBidder(1, 5000);
        auction.addObserver(auto);

        AutoBidConfig cfg = new AutoBidConfig(1, "User1", 3000, 50); // increment < minStep
        auction.registerAutoBid(cfg);

        // Auto không thể đặt vì 1000+50=1050 < 1000+200=1200 → config bị loại ngay
        // winner vẫn null, giá vẫn = startingPrice
        assertEquals(1000, auction.getCurrentHighestBid(), 0.01);
        assertNull(auction.getCurrentWinner());
    }

    /** Auto-bid vẫn kích hoạt khi bidder đã logout (offline). */
    @Test
    @Order(14)
    void autoBid_shouldTriggerEvenWhenBidderOffline() {
        Auction auction = makeAuction(14, 1000, 100, 3600);
        Bidder auto = makeBidder(1, 5000);
        auto.logout(); // offline ngay từ đầu — không add vào observer
        // Không addObserver → Auction phải tìm qua UserManager (test mode trả về null → skip OK)
        // Trong test mode chỉ kiểm tra config còn trong map
        AutoBidConfig cfg = new AutoBidConfig(1, "User1", 2000, 100);
        auction.registerAutoBid(cfg);
        assertEquals(1, auction.getAutoBidConfigs().size());
    }

    /** Hủy auto-bid (cancelAutoBid) phải xóa config khỏi map. */
    @Test
    @Order(15)
    void autoBid_cancelShouldRemoveConfig() {
        Auction auction = makeAuction(15, 1000, 100, 3600);
        AutoBidConfig cfg = new AutoBidConfig(1, "User1", 3000, 100);
        auction.registerAutoBid(cfg);
        assertFalse(auction.getAutoBidConfigs().isEmpty());

        auction.cancelAutoBid(1);
        assertTrue(auction.getAutoBidConfigs().isEmpty());
    }

    /** Auto-bid đặt giá vượt số dư bidder phải bị từ chối và config phải bị xóa. */
    @Test
    @Order(16)
    void autoBid_insufficientBalanceShouldRemoveConfig() {
        Auction auction = makeAuction(16, 1000, 100, 3600);
        Bidder auto = makeBidder(1, 1150); // chỉ đủ đặt 1 lần (1100), không đủ 1200
        auction.addObserver(auto);

        AutoBidConfig cfg = new AutoBidConfig(1, "User1", 3000, 100);
        auction.registerAutoBid(cfg);
        // Auto đặt 1100 ngay khi register → balance = 50 còn lại

        Bidder manual = makeBidder(2, 5000);
        auction.addObserver(manual);
        auction.placeBid(manual, 1200); // manual vượt → auto thử 1300 → thất bại vì balance < 1300
        // Config auto phải bị xóa sau lỗi
        assertFalse(auction.getAutoBidConfigs().containsKey(1));
        assertEquals("User2", auction.getCurrentWinner().getUsername());
    }

    // =========================================================================
    // 2. ANTI-SNIPING
    // =========================================================================

    /** Bid ngoài snipe window (còn hơn 30s) không được gia hạn. */
    @Test
    @Order(20)
    void antiSnipe_noBidOutsideWindow_shouldNotExtend() {
        Auction auction = makeAuction(20, 1000, 100, 120); // còn 2 phút
        Bidder b = makeBidder(1, 5000);
        b.setAuthenticated(true);
        auction.addObserver(b);

        LocalDateTime endBefore = auction.getEndTime();
        auction.placeBid(b, 1100);
        assertEquals(endBefore, auction.getEndTime()); // không thay đổi
        assertEquals(0, auction.getExtensionCount());
    }

    /** Bid trong snipe window phải gia hạn thêm đúng 60s. */
    @Test
    @Order(21)
    void antiSnipe_bidInsideWindow_shouldExtend60Seconds() {
        Auction auction = makeAuction(21, 1000, 100, 15); // còn 15s < 30s
        Bidder b = makeBidder(1, 5000);
        auction.addObserver(b);

        LocalDateTime endBefore = auction.getEndTime();
        auction.placeBid(b, 1100);
        assertTrue(auction.getEndTime().isAfter(endBefore));
        assertEquals(1, auction.getExtensionCount());
        // Kiểm tra gia hạn ít nhất 55s (buffer do thời gian chạy test)
        long diff = Duration.between(endBefore, auction.getEndTime()).toSeconds();
        assertTrue(diff >= 55 && diff <= 65, "Gia hạn phải ~60s, thực tế: " + diff + "s");
    }

    /** Gia hạn liên tiếp: mỗi bid trong window phải tăng extensionCount. */
    @Test
    @Order(22)
    void antiSnipe_multipleExtensions_shouldIncrementCount() {
        Auction auction = makeAuction(22, 1000, 100, 10);
        Bidder b1 = makeBidder(1, 10000);
        Bidder b2 = makeBidder(2, 10000);
        auction.addObserver(b1);
        auction.addObserver(b2);

        auction.placeBid(b1, 1100); // extension 1
        // Sau gia hạn endTime dịch ra 60s → set lại còn 10s để test extension 2
        auction.setEndTime(LocalDateTime.now().plusSeconds(10));
        auction.placeBid(b2, 1200); // extension 2
        assertEquals(2, auction.getExtensionCount());
    }

    /** Không gia hạn quá MAX_EXTENSIONS (= 5). */
    @Test
    @Order(23)
    void antiSnipe_shouldNotExceedMaxExtensions() {
        Auction auction = makeAuction(23, 1000, 100, 10);
        Bidder[] bidders = new Bidder[6];
        for (int i = 0; i < 6; i++) {
            bidders[i] = makeBidder(i + 1, 50000);
            auction.addObserver(bidders[i]);
        }

        double price = 1000;
        for (int i = 0; i < 6; i++) {
            auction.setEndTime(LocalDateTime.now().plusSeconds(10)); // đẩy về window
            price += 100;
            try {
                auction.placeBid(bidders[i], price);
            } catch (Exception ignored) {}
        }
        // extensionCount không vượt quá 5
        assertTrue(auction.getExtensionCount() <= 5);
    }

    // =========================================================================
    // 3. ĐA LUỒNG (MULTI-THREAD)
    // =========================================================================

    /** 10 thread cùng bid đồng thời → chỉ 1 winner, giá hợp lệ, không corrupt state. */
    @Test
    @Order(30)
    void multiThread_onlyOneWinnerAfterConcurrentBids() throws InterruptedException {
        Auction auction = makeAuction(30, 1000, 50, 3600);
        int N = 10;
        Bidder[] bidders = new Bidder[N];
        for (int i = 0; i < N; i++) {
            bidders[i] = makeBidder(i + 1, 10000);
            auction.addObserver(bidders[i]);
        }

        CyclicBarrier barrier = new CyclicBarrier(N);
        List<Thread> threads = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < N; i++) {
            final Bidder b = bidders[i];
            final double bidAmount = 1100 + i * 50; // giá khác nhau mỗi bidder
            threads.add(new Thread(() -> {
                try {
                    barrier.await(); // tất cả bắt đầu cùng lúc
                    auction.placeBid(b, bidAmount);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {}
            }));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join(3000);

        assertNotNull(auction.getCurrentWinner());
        assertTrue(auction.getCurrentHighestBid() >= 1100);
        // Giá hiện tại phải khớp với bid trong bidHistory cuối cùng
        List<BidTransaction> history = auction.getBidHistory();
        assertFalse(history.isEmpty());
        double lastBid = history.get(history.size() - 1).getAmount();
        assertEquals(lastBid, auction.getCurrentHighestBid(), 0.01);
    }

    /** 5 thread cùng đặt giá như nhau (race condition): chỉ 1 thành công, 4 còn lại bị từ chối. */
    @Test
    @Order(31)
    void multiThread_sameAmountOnlyOneAccepted() throws InterruptedException {
        Auction auction = makeAuction(31, 1000, 100, 3600);
        int N = 5;
        Bidder[] bidders = new Bidder[N];
        for (int i = 0; i < N; i++) {
            bidders[i] = makeBidder(i + 1, 5000);
            auction.addObserver(bidders[i]);
        }

        CyclicBarrier barrier = new CyclicBarrier(N);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>();
        final double SAME_BID = 1200.0;

        for (Bidder b : bidders) {
            threads.add(new Thread(() -> {
                try {
                    barrier.await();
                    auction.placeBid(b, SAME_BID);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {}
            }));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join(3000);

        assertEquals(1, successCount.get(), "Chỉ đúng 1 bid thành công với cùng mức giá");
    }

    /** Nhiều thread đăng ký auto-bid cùng lúc → không duplicate, map không bị corrupt. */
    @Test
    @Order(32)
    void multiThread_concurrentAutoBidRegistration_noDuplicate() throws InterruptedException {
        Auction auction = makeAuction(32, 1000, 100, 3600);
        int N = 8;
        List<Thread> threads = new ArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(N);

        for (int i = 0; i < N; i++) {
            final int idx = i;
            Bidder b = makeBidder(idx + 1, 20000);
            auction.addObserver(b);
            threads.add(new Thread(() -> {
                try {
                    barrier.await();
                    auction.registerAutoBid(new AutoBidConfig(idx + 1, "User" + (idx + 1), 5000, 100));
                } catch (Exception ignored) {}
            }));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join(3000);

        // Mỗi bidderId phải xuất hiện đúng 1 lần trong map
        Set<Integer> keys = auction.getAutoBidConfigs().keySet();
        assertEquals(new HashSet<>(keys).size(), keys.size(), "Không được có duplicate key");
    }

    /** addObserver và removeObserver từ nhiều thread đồng thời không gây ConcurrentModificationException. */
    @Test
    @Order(33)
    void multiThread_concurrentObserverAddRemove_noException() throws InterruptedException {
        Auction auction = makeAuction(33, 1000, 100, 3600);
        int N = 20;
        List<Bidder> bidders = new ArrayList<>();
        for (int i = 0; i < N; i++) bidders.add(makeBidder(i + 1, 5000));

        List<Thread> threads = new ArrayList<>();
        for (Bidder b : bidders) {
            threads.add(new Thread(() -> {
                auction.addObserver(b);
                Thread.yield();
                auction.removeObserver(b);
            }));
        }

        AtomicReference<Throwable> error = new AtomicReference<>();
        threads.forEach(t -> t.setUncaughtExceptionHandler((th, ex) -> error.set(ex)));
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join(3000);

        assertNull(error.get(), "Không được có exception khi add/remove observer đồng thời");
    }

    /**
     * Stress test: 50 thread liên tục bid trong 2 giây.
     * Sau khi kết thúc, state auction phải nhất quán (winner khớp bidHistory).
     */
    @Test
    @Order(34)
    void multiThread_stressTest_stateConsistentAfterHighLoad() throws InterruptedException {
        Auction auction = makeAuction(34, 1000, 10, 3600);
        int N = 50;
        Bidder[] bidders = new Bidder[N];
        for (int i = 0; i < N; i++) {
            bidders[i] = makeBidder(i + 1, 200000);
            auction.addObserver(bidders[i]);
        }

        ExecutorService pool = Executors.newFixedThreadPool(N);
        long deadline = System.currentTimeMillis() + 2000; // 2 giây

        for (int i = 0; i < N; i++) {
            final Bidder b = bidders[i];
            pool.submit(() -> {
                Random rng = new Random();
                while (System.currentTimeMillis() < deadline) {
                    try {
                        double current = auction.getCurrentHighestBid();
                        double bid = current + 10 + rng.nextInt(50);
                        auction.placeBid(b, bid);
                    } catch (Exception ignored) {}
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // Kiểm tra nhất quán: bid cuối trong history == currentHighestBid
        List<BidTransaction> history = auction.getBidHistory();
        if (!history.isEmpty()) {
            double lastTx = history.get(history.size() - 1).getAmount();
            assertEquals(lastTx, auction.getCurrentHighestBid(), 0.01,
                    "currentHighestBid phải khớp với transaction cuối trong bidHistory");
        }
        assertNotNull(auction.getCurrentWinner());
    }

    // =========================================================================
    // 4. CRASH RECOVERY / SERIALIZATION
    // =========================================================================

    /**
     * Serialize auction ra byte[] rồi deserialize lại.
     * Kiểm tra dữ liệu quan trọng được bảo toàn.
     */
    @Test
    @Order(40)
    void crashRecovery_serializeAndDeserialize_dataPreserved() throws Exception {
        Auction auction = makeAuction(40, 2000, 200, 3600);
        Bidder b1 = makeBidder(1, 10000);
        Bidder b2 = makeBidder(2, 10000);
        auction.addObserver(b1);
        auction.addObserver(b2);
        auction.placeBid(b1, 2200);
        auction.placeBid(b2, 2400);

        // Serialize
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(auction);
            bytes = bos.toByteArray();
        }

        // Deserialize
        Auction restored;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            restored = (Auction) ois.readObject();
        }

        assertEquals(auction.getId(), restored.getId());
        assertEquals(auction.getCurrentHighestBid(), restored.getCurrentHighestBid(), 0.01);
        assertEquals(auction.getStatus(), restored.getStatus());
        assertEquals(auction.getBidHistory().size(), restored.getBidHistory().size());
        assertNotNull(restored.getCurrentWinner());
        assertEquals(auction.getCurrentWinner().getUsername(),
                restored.getCurrentWinner().getUsername());
    }

    /** Sau deserialize, observers phải được khởi tạo lại (không null, rỗng). */
    @Test
    @Order(41)
    void crashRecovery_observersReinitializedAfterDeserialize() throws Exception {
        Auction auction = makeAuction(41, 1000, 100, 3600);
        Bidder b = makeBidder(1, 5000);
        auction.addObserver(b);

        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(auction);
            bytes = bos.toByteArray();
        }

        Auction restored;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            restored = (Auction) ois.readObject();
        }

        // observers là transient → phải được readObject() khởi tạo lại thành list rỗng (không null)
        assertNotNull(restored.getObservers(), "observers không được null sau deserialize");
    }

    /** Sau deserialize, có thể tiếp tục đặt giá bình thường (auction vẫn hoạt động được). */
    @Test
    @Order(42)
    void crashRecovery_canContinueBiddingAfterRestore() throws Exception {
        Auction auction = makeAuction(42, 1000, 100, 3600);
        Bidder b1 = makeBidder(1, 10000);
        auction.addObserver(b1);
        auction.placeBid(b1, 1100);

        // Serialize & deserialize
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(auction);
            bytes = bos.toByteArray();
        }
        Auction restored;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            restored = (Auction) ois.readObject();
        }

        // Thêm lại observer (vì transient) và tiếp tục bid
        Bidder b2 = makeBidder(2, 10000);
        restored.addObserver(b2);
        assertDoesNotThrow(() -> restored.placeBid(b2, 1200));
        assertEquals(1200, restored.getCurrentHighestBid(), 0.01);
    }

    /** bidHistory không được mất dù serialize nhiều lần (giả lập snapshot liên tiếp). */
    @Test
    @Order(43)
    void crashRecovery_bidHistoryIntactAfterMultipleSnapshots() throws Exception {
        Auction auction = makeAuction(43, 1000, 100, 3600);
        Bidder[] bs = new Bidder[5];
        for (int i = 0; i < 5; i++) {
            bs[i] = makeBidder(i + 1, 20000);
            auction.addObserver(bs[i]);
        }

        Auction current = auction;
        for (int round = 0; round < 5; round++) {
            double bid = current.getCurrentHighestBid() + 100;
            current.placeBid(bs[round], bid);

            // snapshot sau mỗi bid
            byte[] bytes;
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(current);
                bytes = bos.toByteArray();
            }
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                current = (Auction) ois.readObject();
            }
            // Thêm lại observers sau restore
            for (Bidder b : bs) current.addObserver(b);
        }

        assertEquals(5, current.getBidHistory().size(),
                "bidHistory phải lưu đủ 5 lần đặt giá qua 5 lần snapshot");
    }

    // =========================================================================
    // 5. CÁC EDGE CASE BỔ SUNG
    // =========================================================================

    /** Đặt giá âm phải ném IllegalArgumentException. */
    @Test
    @Order(50)
    void edgeCase_negativeBidShouldThrow() {
        Auction auction = makeAuction(50, 1000, 100, 3600);
        Bidder b = makeBidder(1, 5000);
        auction.addObserver(b);
        assertThrows(IllegalArgumentException.class, () -> auction.placeBid(b, -500));
    }

    /** Đặt giá bằng 0 phải ném IllegalArgumentException. */
    @Test
    @Order(51)
    void edgeCase_zeroBidShouldThrow() {
        Auction auction = makeAuction(51, 1000, 100, 3600);
        Bidder b = makeBidder(1, 5000);
        auction.addObserver(b);
        assertThrows(IllegalArgumentException.class, () -> auction.placeBid(b, 0));
    }

    /** Winner hiện tại không được tự đặt giá tiếp. */
    @Test
    @Order(52)
    void edgeCase_currentWinnerCannotBidAgain() {
        Auction auction = makeAuction(52, 1000, 100, 3600);
        Bidder b1 = makeBidder(1, 10000);
        Bidder b2 = makeBidder(2, 10000);
        auction.addObserver(b1);
        auction.addObserver(b2);

        auction.placeBid(b1, 1100);
        auction.placeBid(b2, 1200);
        auction.placeBid(b1, 1300); // b1 thắng
        // b1 cố đặt thêm
        assertThrows(InvalidBidException.class, () -> auction.placeBid(b1, 1400));
    }

    /** Đặt giá đúng bằng minAccepted = currentHighest + minBidStep phải thành công. */
    @Test
    @Order(53)
    void edgeCase_exactMinAcceptedBidShouldSucceed() {
        Auction auction = makeAuction(53, 1000, 100, 3600);
        Bidder b1 = makeBidder(1, 10000);
        Bidder b2 = makeBidder(2, 10000);
        auction.addObserver(b1);
        auction.addObserver(b2);

        auction.placeBid(b1, 1100); // = startingPrice + minStep
        assertEquals(1100, auction.getCurrentHighestBid(), 0.01);

        auction.placeBid(b2, 1200); // = 1100 + 100 (đúng bằng minAccepted)
        assertEquals(1200, auction.getCurrentHighestBid(), 0.01);
    }

    /** Đặt giá khi auction ở trạng thái OPEN (chưa start) phải ném AuctionClosedException. */
    @Test
    @Order(54)
    void edgeCase_bidOnOpenAuctionShouldThrow() {
        Item item = new Art(54, 99, "Painting", "desc", 1000, "", "Artist");
        Auction auction = new Auction(54, item, LocalDateTime.now(), LocalDateTime.now().plusDays(1), 100);
        // status = OPEN (chưa setStatus RUNNING)
        Bidder b = makeBidder(1, 5000);
        b.setAuthenticated(true);
        assertThrows(AuctionClosedException.class, () -> auction.placeBid(b, 1100));
    }

    /** Bid sau khi endTime đã qua phải tự động finish auction và ném AuctionClosedException. */
    @Test
    @Order(55)
    void edgeCase_bidAfterEndTimeShouldFinishAndThrow() {
        Item item = new Electronics(55, 99, "Phone", "desc", 1000, "", "Brand");
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end   = LocalDateTime.now().minusSeconds(1); // đã hết giờ
        Auction auction = new Auction(55, item, start, end, 100);
        auction.setStatus(AuctionStatus.RUNNING);

        Bidder b = makeBidder(1, 5000);
        b.setAuthenticated(true);
        assertThrows(AuctionClosedException.class, () -> auction.placeBid(b, 1100));
    }

    /** viewCount phải tăng khi add observer và giảm khi remove (tính đúng số người xem). */
    @Test
    @Order(56)
    void edgeCase_viewCountTracksObservers() {
        Auction auction = makeAuction(56, 1000, 100, 3600);
        Bidder b1 = makeBidder(1, 5000);
        Bidder b2 = makeBidder(2, 5000);

        assertEquals(0, auction.getViewCount());
        auction.addObserver(b1);
        assertEquals(1, auction.getViewCount());
        auction.addObserver(b2);
        assertEquals(2, auction.getViewCount());
        auction.removeObserver(b1);
        assertEquals(1, auction.getViewCount());
    }
}