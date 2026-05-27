package com.example.group11.controller;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class BidderFilterManager {

    public static List<Auction> filter(List<Auction> allAuctions, User user, String currentTab,
                                       String searchBarText, String statusFilter, String categoryFilter, String sortFilter) {
        if (allAuctions == null) return List.of();

        String query = (searchBarText != null) ? searchBarText.trim().toLowerCase() : "";
        String statusUpper = statusFilter != null ? statusFilter.trim().toUpperCase() : "TRẠNG THÁI";
        String categoryUpper = categoryFilter != null ? categoryFilter.trim().toUpperCase() : "SẢN PHẨM";

        List<Auction> filtered = allAuctions.stream().filter(auction -> {
            if (currentTab.equals("DASHBOARD")) {
                if (auction.getStatus() != AuctionStatus.RUNNING
                        && auction.getStatus() != AuctionStatus.OPEN
                        && auction.getStatus() != AuctionStatus.FINISHED
                        && auction.getStatus() != AuctionStatus.PAID) {
                    return false;
                }
                if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
                    if (auction.getEndTime() != null && auction.getEndTime().plusDays(10).isBefore(LocalDateTime.now())) {
                        return false;
                    }
                }
            } else if (currentTab.equals("MY BIDS")) {
                if (user instanceof Bidder bidder) {
                    boolean bidPlaced = bidder.getProfile().getParticipatedAuctions().contains(auction.getId()) ||
                            auction.getBidHistory().stream().anyMatch(tx -> tx.getBidderId() == bidder.getId());
                    if (!bidPlaced) return false;
                } else {
                    return false;
                }
            } else if (currentTab.equals("WATCHLIST")) {
                if (user instanceof Bidder bidder) {
                    List<Integer> wl = bidder.getProfile().getWatchlist();
                    if (wl == null || !wl.contains(auction.getId())) return false;
                } else {
                    return false;
                }
            } else if (currentTab.equals("HISTORY")) {
                if (auction.getStatus() != AuctionStatus.FINISHED
                        && auction.getStatus() != AuctionStatus.CANCELED
                        && auction.getStatus() != AuctionStatus.PAID) {
                    return false;
                }
                if (user instanceof Bidder bidder) {
                    boolean won = (auction.getCurrentWinner() != null && auction.getCurrentWinner().getId() == bidder.getId())
                            || bidder.getProfile().getWonAuctions().contains(auction.getId());
                    boolean participated = bidder.getProfile().getParticipatedAuctions().contains(auction.getId())
                            || auction.getBidHistory().stream().anyMatch(tx -> tx.getBidderId() == bidder.getId());
                    if (!won && !participated) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            if (!statusUpper.equals("TRẠNG THÁI") && !statusUpper.equals("TẤT CẢ")) {
                if (!auction.getStatus().toString().equals(statusUpper)) {
                    return false;
                }
            }

            if (!categoryUpper.equals("SẢN PHẨM") && !categoryUpper.equals("TẤT CẢ")) {
                if (auction.getItem() == null || !auction.getItem().getCategory().toUpperCase().equals(categoryUpper)) {
                    return false;
                }
            }

            if (!query.isEmpty()) {
                String idStr = String.valueOf(auction.getId());
                String itemName = (auction.getItem() != null && auction.getItem().getName() != null)
                        ? auction.getItem().getName().toLowerCase() : "";
                String itemDesc = (auction.getItem() != null && auction.getItem().getDescription() != null)
                        ? auction.getItem().getDescription().toLowerCase() : "";

                if (!idStr.contains(query) && !itemName.contains(query) && !itemDesc.contains(query)) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());
        
        String sortUpper = sortFilter != null ? sortFilter.trim().toUpperCase() : "SẮP XẾP";
        if (sortUpper.equals("GIÁ: THẤP ĐẾN CAO")) {
            filtered.sort((a, b) -> Double.compare(a.getCurrentHighestBid(), b.getCurrentHighestBid()));
        } else if (sortUpper.equals("GIÁ: CAO ĐẾN THẤP")) {
            filtered.sort((a, b) -> Double.compare(b.getCurrentHighestBid(), a.getCurrentHighestBid()));
        } else if (sortUpper.equals("KẾT THÚC SỚM NHẤT")) {
            filtered.sort((a, b) -> {
                LocalDateTime timeA = a.getEndTime() != null ? a.getEndTime() : LocalDateTime.MAX;
                LocalDateTime timeB = b.getEndTime() != null ? b.getEndTime() : LocalDateTime.MAX;
                return timeA.compareTo(timeB);
            });
        } else {
            // Mới nhất (default)
            filtered.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        }
        return filtered;
    }
}
