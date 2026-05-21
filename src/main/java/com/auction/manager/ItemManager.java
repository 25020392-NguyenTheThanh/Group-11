package com.auction.manager;

import com.auction.data.DataManager;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.pattern.factory.ItemFactory;

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
        // Tạo tạm với id=0 để factory build đúng kiểu
        Item temp = factory.createItem(0, ownerId, name, description, startingPrice, imageUrl);

        // Lưu vào MySQL — nhận id thật
        int dbId = DataManager.getInstance().addItem(temp);
        if (dbId == -1) {
            System.out.println("Lỗi lưu item vào DB: " + name);
            return null;
        }

        // Tạo lại với id thật
        Item item = factory.createItem(dbId, ownerId, name, description, startingPrice, imageUrl );
        items.put(dbId, item);

        System.out.println("Sản phẩm mới: " + item.getInfo());
        return item;
    }

    // Lấy item theo id — kiểm tra RAM trước, fallback về MySQL.
    public Item findItem(int id) {
        Item item = items.get(id);
        if (item != null) return item;

        // Fallback: tìm trong DB rồi cache lại
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

    // Lấy tất cả item đang AVAILABLE.
    public List<Item> getAvailableItems() {
        return items.values().stream()
                .filter(i -> i.getStatus() == ItemStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    // Lấy item theo owner — từ MySQL để đảm bảo đủ dữ liệu ngay cả sau restart.
    public List<Item> getByOwner(int ownerId) {
        List<Item> fromDb = DataManager.getInstance().getItemsBySeller(ownerId);
        // Đồng bộ cache
        fromDb.forEach(i -> items.put(i.getId(), i));
        return fromDb;
    }

    // Xóa sản phẩm theo id khỏi database và cache.
    public boolean deleteItem(int id) {
        boolean success = DataManager.getInstance().deleteItem(id);
        if (success) {
            items.remove(id);
        }
        return success;
    }

    // Cập nhật sản phẩm.
    public boolean updateItem(Item item) {
        boolean success = DataManager.getInstance().updateItem(item);
        if (success) {
            items.put(item.getId(), item);
        }
        return success;
    }

    // Cập nhật trạng thái sản phẩm (đồng bộ cache và DB).
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
}