package com.auction.model.auction;

import com.auction.data.DataManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class AntiSnipeGuard {

    private static final long SNIPE_WINDOW_SECONDS = 30;
    private static final long EXTENSION_SECONDS    = 60;
    private static final int  MAX_EXTENSIONS       = 5;

    private final Auction auction;

    AntiSnipeGuard(Auction auction) {
        this.auction = auction;
    }

    void checkAndExtend() {
        if (auction.getExtensionCount() >= MAX_EXTENSIONS) return;
        long secondsLeft = Duration.between(LocalDateTime.now(), auction.getEndTime()).toSeconds();
        if (secondsLeft <= 0 || secondsLeft > SNIPE_WINDOW_SECONDS) return;

        auction.setEndTime(auction.getEndTime().plusSeconds(EXTENSION_SECONDS));
        auction.incrementExtensionCount();

        if (!DataManager.isTestMode())
            DataManager.getInstance().updateAuctionEndTime(auction.getId(), auction.getEndTime());

        String msg = String.format(
                "Anti-Snipe! Phiên #%d được gia hạn thêm %ds (lần %d/%d) — kết thúc lúc %s",
                auction.getId(), EXTENSION_SECONDS, auction.getExtensionCount(), MAX_EXTENSIONS,
                auction.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        System.out.println(msg);
        auction.notifyObservers(msg);
        auction.notifyObservers("TIME_EXTENDED:" + auction.getEndTime().toString());
    }
}