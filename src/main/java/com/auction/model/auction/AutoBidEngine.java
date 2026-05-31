package com.auction.model.auction;

import com.auction.data.DataManager;
import com.auction.manager.UserManager;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.pattern.observer.Observer;
import com.auction.server.ClientHandler;

import java.util.concurrent.TimeUnit;

class AutoBidEngine {

    private final Auction auction;

    AutoBidEngine(Auction auction) {
        this.auction = auction;
    }

    void triggerForConfig(AutoBidConfig config) {
        // Chỉ tìm trong observer list — KHÔNG gọi UserManager (tránh DB call khi test/offline)
        Bidder bidder = resolveBidderFromObservers(config.getBidderId());
        if (bidder != null) trigger(bidder);
    }

    synchronized void trigger(Bidder justBidder) {
        if (auction.getStatus() != AuctionStatus.RUNNING) return;

        AutoBidConfig best = null;
        double bestNextBid = 0;
        for (AutoBidConfig cfg : auction.getAutoBidConfigs().values()) {
            if (auction.getCurrentWinner() != null && cfg.getBidderId() == auction.getCurrentWinner().getId()) continue;
            double nextBid = auction.getCurrentHighestBid() + cfg.getIncrement();
            if (nextBid > cfg.getMaxBid()) continue;
            if (best == null || cfg.getMaxBid() > best.getMaxBid()) {
                best = cfg;
                bestNextBid = nextBid;
            }
        }
        if (best == null) return;

        // trigger() được gọi từ manual bid path → có thể dùng UserManager làm fallback
        Bidder bidder = resolveBidder(best.getBidderId());
        if (bidder == null) return;

        final Bidder activeBidder = bidder;
        final double nextBid = bestNextBid;
        final int bestBidderId = best.getBidderId();

        try {
            new BidProcessor(auction).execute(activeBidder, nextBid, true);

            if (DataManager.isTestMode()) {
                trigger(activeBidder);
            } else {
                auction.getAutoBidExecutor().schedule(() -> trigger(activeBidder), 500, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            System.err.println("[AutoBid] Lỗi khi xử lý Auto-Bid của @" + activeBidder.getUsername() + ": " + e.getMessage());
            auction.getAutoBidConfigs().remove(bestBidderId);

            if (DataManager.isTestMode()) {
                trigger(activeBidder);
            } else {
                auction.getAutoBidExecutor().schedule(() -> trigger(activeBidder), 500, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        }
    }

    // Tìm trong observer list + UserManager fallback (dùng cho manual bid path)
    private Bidder resolveBidder(int bidderId) {
        Bidder b = resolveBidderFromObservers(bidderId);
        if (b != null) return b;
        User u = UserManager.getInstance().findUserById(bidderId);
        return (u instanceof Bidder bidder) ? bidder : null;
    }

    // Chỉ tìm trong observer list, không chạm DB
    private Bidder resolveBidderFromObservers(int bidderId) {
        for (Observer obs : auction.getObservers()) {
            if (obs instanceof Bidder b && b.getId() == bidderId) return b;
            if (obs instanceof ClientHandler handler) {
                User u = handler.getLoggedInUser();
                if (u instanceof Bidder b && b.getId() == bidderId) return b;
            }
        }
        return null;
    }
}