package com.auction.model.item;

import com.auction.model.entity.Entity;

// thêm ownerId
public abstract class Item extends Entity {
    private String name;
    private String description;  // mô tả chi tiết về sản phầm
    private double startingPrice; // giá khởi điểm
    private ItemStatus status;
    private final int ownerId; // id của Seller tạo ra Item
    private String imageUrl; // link ảnh sản phẩm

    public Item(int id, int ownerId, String name, String description, double startingPrice, String imageUrl) {
        super(id);
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.status = ItemStatus.AVAILABLE;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // trả về loại sản phẩm : Art , Electronics , Vehicle
    public abstract String getCategory();

    @Override
    public String getInfo() {
        return "[" + getCategory() + "] : " + name + " - " + getId();
    }
}
