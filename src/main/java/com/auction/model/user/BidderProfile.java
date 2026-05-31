package com.auction.model.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BidderProfile implements Serializable {
    private final int bidderId;
    private final List<Integer> wonAuctions; // danh sách phiên đã thắng
    private final List<Integer> watchList; // danh sách phiên đang theo dõi
    private final List<Integer> participatedAuctions; // phiên đã từng đặt giá

    public BidderProfile(int bidderId) {
        this.bidderId = bidderId;
        this.wonAuctions = new ArrayList<>();
        this.watchList = new ArrayList<>();
        this.participatedAuctions = new ArrayList<>();
    }
    // watchList
    public void addToWatchlist(int auctionId) {
        if (!watchList.contains(auctionId)) watchList.add(auctionId);
    }

    public void removeFromWatchlist(int auctionId) {
        watchList.remove(Integer.valueOf(auctionId));
    }

    public List<Integer> getWatchlist() {
        return watchList;
    }

    // ParticipatedAuctions
    public void addParticipatedAuction(int auctionId) {
        if (!participatedAuctions.contains(auctionId))
            participatedAuctions.add(auctionId);
    }

    public List<Integer> getParticipatedAuctions() {
        return participatedAuctions;
    }

    // WonAuctions
    public void addWonAuction(int auctionId) {
        wonAuctions.add(auctionId);
    }

    public List<Integer> getWonAuctions() { return wonAuctions; }
}
