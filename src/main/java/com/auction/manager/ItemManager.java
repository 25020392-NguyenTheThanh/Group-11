package com.auction.manager;

import com.auction.data.ItemDAO;
import com.auction.model.item.*;
import com.auction.pattern.factory.ItemFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ItemManager {
    private static volatile ItemManager instance;
    private final ConcurrentHashMap<Integer, Item> items = new ConcurrentHashMap<>();
    private final AtomicInteger itemCounter = new AtomicInteger(1);// tránh Rase condition khi counter++
    private final ItemDAO itemDAO = new ItemDAO();

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
    //nhận ItemFactory thay vì String type
    public Item createItem(ItemFactory factory, int ownerId,
                           String name, String description, double startingPrice) {
        int id = itemCounter.getAndIncrement();
        Item item = factory.createItem(id, ownerId, name, description, startingPrice);
        int realId = itemDAO.save(item); // lưu vào MySQL , lấy id thật
        if (realId == -1 ){
            System.err.println("Lỗi lưu item vào DB");
            return null ;
        }
        Item savedItem = factory.createItem(realId , ownerId ,name , description , startingPrice);
        items.put(realId , savedItem);
        System.out.println("Sản phẩm mới: " + item.getInfo());
        return savedItem;
    }

    public List<Item> getAvailableItems() {
        return items.values().stream()
                .filter(i -> i.getStatus() == ItemStatus.AVAILABLE)
                .collect(Collectors.toList());
    }
    public List<Item> getByOwner(int ownerId) {
        return items.values().stream()
                .filter(i -> i.getOwnerId() == ownerId)
                .collect(Collectors.toList());
    }

    public Item findItem(int id) {
        Item item = items.get(id);
        if (item == null) System.out.println("Sản phẩm không tồn tại: " + id);
        return item;
    }
    public void loadFromDB(){
        List<Item> list = itemDAO.findAll();
        items.clear();
        list.forEach(i -> items.put(i.getId(),i));
        if (!list.isEmpty()){
            int maxId = list.stream().mapToInt(Item::getId).max().getAsInt();
        }
        System.out.println("Đã load " + items.size() + " items từ MySQL");
    }
}

