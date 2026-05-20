package com.example.group11.controller;

import java.util.ArrayList;
import java.util.List;

public class AuctionItemMock {
    private String id;
    private String name;
    private String desc;
    private String startPrice;
    private String attributeKey;
    private String attributeValue;
    private String status;
    private String imageUrl;

    // Constructor đầy đủ tham số khớp với Factory
    public AuctionItemMock(String id, String name, String desc, String startPrice,
                           String attributeKey, String attributeValue, String status, String imageUrl) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.startPrice = startPrice;
        this.attributeKey = attributeKey;
        this.attributeValue = attributeValue;
        this.status = status;
        this.imageUrl = imageUrl;
    }

    // ==========================================
    // GETTERS & SETTERS
    // ==========================================
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public String getStartPrice() { return startPrice; }
    public void setStartPrice(String startPrice) { this.startPrice = startPrice; }

    public String getAttributeKey() { return attributeKey; }
    public void setAttributeKey(String attributeKey) { this.attributeKey = attributeKey; }

    public String getAttributeValue() { return attributeValue; }
    public void setAttributeValue(String attributeValue) { this.attributeValue = attributeValue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // ==========================================
    // HÀM TẠO DỮ LIỆU GIẢ LẬP ĐỂ TEST NHANH GIAO DIỆN
    // ==========================================
    public static List<AuctionItemMock> getMockList() {
        List<AuctionItemMock> list = new ArrayList<>();

        list.add(new AuctionItemMock(
                "AU-2026-001",
                "LAPTOP GAMING ASUS ROG ZEPHYRUS",
                "Chip M3 Max, 32GB RAM, SSD 1TB, màn hình 120Hz...",
                "55.000.000",
                "Category", "Electronics",
                "RUNNING",
                "https://your-image-url.com/laptop.png"
        ));

        list.add(new AuctionItemMock(
                "AU-2026-002",
                "TRANH SƠN DẦU 'MÙA THU VÀNG'",
                "Tác phẩm độc bản họa sĩ Trần Kiên năm 2024, khung gỗ cao cấp.",
                "12.500.000",
                "Category", "Art",
                "RUNNING",
                "https://your-image-url.com/art.png"
        ));

        list.add(new AuctionItemMock(
                "AU-2026-003",
                "XE MÁY VESPA SPRINT VINTAGE",
                "Phiên bản giới hạn nhập khẩu Ý, chạy lướt 2000km mới 99%.",
                "89.000.000",
                "Category", "Vehicle",
                "RUNNING",
                "https://your-image-url.com/vespa.png"
        ));

        return list;
    }
}