package com.auction.manager;

import com.auction.data.DataManager;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.pattern.factory.ItemFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ItemManager {

    private static volatile ItemManager instance;

    // Runtime cache — key = itemId
    private final ConcurrentHashMap<Integer, Item> items = new ConcurrentHashMap<>();

    private ItemManager() {}

    public static ItemManager getInstance() {
        if (instance == null) {
            synchronized (ItemManager.class) {
                if (instance == null) {
                    instance = new ItemManager();
                }
            }
        }
        return instance;
    }

    /**
     * Tạo Item mới bằng factory, lưu vào MySQL, giữ trong RAM.
     * return Item vừa tạo (id là id thật từ DB).
     */
    public Item createItem(ItemFactory factory, int ownerId,
                           String name, String description, double startingPrice, String imageUrl) {
        Item temp = factory.createItem(0, ownerId, name, description, startingPrice, imageUrl);

        int dbId = DataManager.getInstance().addItem(temp);
        if (dbId == -1) {
            System.out.println("Lỗi lưu item vào DB: " + name);
            return null;
        }

        Item item = factory.createItem(dbId, ownerId, name, description, startingPrice, imageUrl );
        items.put(dbId, item);

        System.out.println("Sản phẩm mới: " + item.getInfo());
        return item;
    }

    public Item findItem(int id) {
        Item item = items.get(id);
        if (item != null) return item;

        List<Item> all = DataManager.getInstance().getAllItems();
        for (Item i : all) {
            if (i.getId() == id) {
                items.put(id, i);
                return i;
            }
        }
        System.out.println("Sản phẩm không tồn tại: " + id);
        return null;
    }

    public List<Item> getAvailableItems() {
        return items.values().stream()
                .filter(i -> i.getStatus() == ItemStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    public List<Item> getByOwner(int ownerId) {
        List<Item> fromDb = DataManager.getInstance().getItemsBySeller(ownerId);
        fromDb.forEach(i -> items.put(i.getId(), i));
        return fromDb;
    }

    public boolean deleteItem(int id) {
        boolean success = DataManager.getInstance().deleteItem(id);
        if (success) {
            items.remove(id);
            AuctionManager.getInstance().getAuctions().stream()
                    .filter(a -> a.getItem() != null && a.getItem().getId() == id)
                    .findFirst()
                    .ifPresent(a -> AuctionManager.getInstance().removeAuctionFromCache(a.getId()));
        }
        return success;
    }

    public boolean updateItem(Item item) {
        boolean success = DataManager.getInstance().updateItem(item);
        if (success) {
            items.put(item.getId(), item);
        }
        return success;
    }

    public boolean updateItemStatus(int itemId, ItemStatus status) {
        boolean success = DataManager.getInstance().updateItemStatus(itemId, status);
        if (success) {
            Item item = findItem(itemId);
            if (item != null) {
                item.setStatus(status);
            }
        }
        return success;
    }

    // =========================================================================
    // --- CÁC HÀM BỔ SUNG DÀNH CHO PHÂN HỆ ADMIN ---
    // =========================================================================

    /**
     * Phục vụ: handleAdminGetAllItems
     */
    public List<Item> getAllItemsInSystem() {
        List<Item> allItems = DataManager.getInstance().getAllItems();
        if (allItems != null) {
            allItems.forEach(i -> items.put(i.getId(), i));
            return allItems;
        }
        return new ArrayList<>();
    }

    /**
     * Phục vụ: handleAdminForceDeleteItem
     */
    public boolean forceDeleteItemByAdmin(int itemId) {
        Item item = findItem(itemId);
        if (item == null) return false;

        // Xử lý ràng buộc nếu mặt hàng đang trong một phiên đấu giá tích cực
        if (item.getStatus() == ItemStatus.IN_AUCTION) {
            AuctionManager.getInstance().getAuctions().stream()
                    .filter(a -> a.getItem() != null && a.getItem().getId() == itemId)
                    .findFirst()
                    .ifPresent(a -> {
                        AuctionManager.getInstance().cancelAuctionByAdmin(a.getId(), "Sản phẩm bị cưỡng chế xóa bởi Admin.");
                        AuctionManager.getInstance().removeAuctionFromCache(a.getId());
                    });
        }

        boolean success = DataManager.getInstance().deleteItem(itemId);
        if (success) {
            items.remove(itemId);
            System.out.println("[ItemManager] Admin đã cưỡng chế xóa thành công sản phẩm ID: " + itemId);
        }
        return success;
    }
}