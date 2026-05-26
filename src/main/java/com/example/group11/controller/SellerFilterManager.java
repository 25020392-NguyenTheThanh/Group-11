package com.example.group11.controller;

import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SellerFilterManager {

    public static List<Item> filterAndSort(List<Item> auctionItems, Map<Integer, Auction> itemAuctionMap,
                                           String query, String selectedStatus, String selectedSort) {
        if (auctionItems == null) return new ArrayList<>();

        String trimmedQuery = query != null ? query.trim().toLowerCase() : "";
        String statusUpper = selectedStatus != null ? selectedStatus.trim().toUpperCase() : "TRẠNG THÁI";

        List<Item> filteredItems = new ArrayList<>();
        for (Item item : auctionItems) {
            boolean matchesSearch = trimmedQuery.isEmpty() ||
                    String.valueOf(item.getId()).contains(trimmedQuery) ||
                    (item.getName() != null && item.getName().toLowerCase().contains(trimmedQuery)) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(trimmedQuery));

            if (!matchesSearch) continue;

            boolean matchesStatus = true;
            if (!statusUpper.equals("TẤT CẢ") && !statusUpper.equals("TRẠNG THÁI")) {
                matchesStatus = (item.getStatus() != null &&
                        item.getStatus().name().equalsIgnoreCase(statusUpper));
            }

            if (matchesStatus) {
                filteredItems.add(item);
            }
        }

        String sortUpper = selectedSort != null ? selectedSort.trim().toUpperCase() : "SẮP XẾP";
        if (sortUpper.equals("GIÁ: THẤP ĐẾN CAO")) {
            filteredItems.sort(Comparator.comparingDouble(Item::getStartingPrice));
        } else if (sortUpper.equals("GIÁ: CAO ĐẾN THẤP")) {
            filteredItems.sort((a, b) -> Double.compare(b.getStartingPrice(), a.getStartingPrice()));
        } else if (sortUpper.equals("KẾT THÚC SỚM NHẤT")) {
            filteredItems.sort((a, b) -> {
                Auction auctionA = itemAuctionMap.get(a.getId());
                Auction auctionB = itemAuctionMap.get(b.getId());
                LocalDateTime timeA = (auctionA != null && auctionA.getEndTime() != null) ? auctionA.getEndTime() : LocalDateTime.MAX;
                LocalDateTime timeB = (auctionB != null && auctionB.getEndTime() != null) ? auctionB.getEndTime() : LocalDateTime.MAX;
                return timeA.compareTo(timeB);
            });
        } else {
            filteredItems.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        }

        return filteredItems;
    }
}
