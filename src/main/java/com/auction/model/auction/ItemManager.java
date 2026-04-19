package com.auction.model.auction;

import com.auction.model.item.*;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {
    private static volatile ItemManager instance;
    private List<Item> items = new ArrayList<>();
    private int itemCounter = 1;

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

    // tạo sản phẩm mới
    public Item createItem(String type, String name,
                           String desc, double startPrice) {
        int id = itemCounter++;
        Item newItem;
        if (type.equals("ELECTRONICS")) {
            newItem = new Electronics(id, name, desc, startPrice, "");
        } else if (type.equals("ART")) {
            newItem = new Art(id, name, desc, startPrice, "");
        } else {
            newItem = new Vehicle(id, name, desc, startPrice, 0);
        }
        items.add(newItem);
        return newItem;
    }

    public List<Item> getAvailableItems() {
        List<Item> result = new ArrayList<>();
        for (Item item : items) {
            if (item.getStatus() == ItemStatus.AVAILABLE) {
                result.add(item);
            }
        }
        return result;
    }

    public Item findItem(int id) {
        for (Item item : items) {
            if (item.getId() == id) return item;
        }
        System.out.println("Sản phẩm không tồn tại");
        return null;
    }
}
