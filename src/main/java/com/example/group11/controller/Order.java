package com.example.group11.controller;

import java.time.LocalDateTime;

/**
 * Lớp đại diện cho một Đơn hàng (Giao dịch hoàn thành) trong danh sách lịch sử giao dịch của người bán.
 */
public class Order {
    // Mã đơn hàng (ID phiên đấu giá)
    private final int id;
    // Tên sản phẩm đấu giá
    private final String product;
    // Tên người mua thắng cuộc
    private final String buyer;
    // Giá chốt phiên đấu giá
    private final double price;
    // Thời điểm phiên đấu giá kết thúc hoặc thanh toán thành công
    private final LocalDateTime date;
    // Trạng thái đơn hàng (ví dụ: FINISHED, PAID)
    private final String status;

    /**
     * Khởi tạo một đối tượng Đơn hàng mới.
     *
     * @param id      Mã đơn hàng
     * @param product Tên sản phẩm
     * @param buyer   Tên người mua
     * @param price   Giá chốt
     * @param date    Ngày hoàn thành
     * @param status  Trạng thái giao dịch
     */
    public Order(int id, String product, String buyer, double price, LocalDateTime date, String status) {
        this.id = id;
        this.product = product;
        this.buyer = buyer;
        this.price = price;
        this.date = date;
        this.status = status;
    }

    /**
     * Lấy mã đơn hàng.
     * @return mã đơn hàng (ID)
     */
    public int getId() {
        return id;
    }

    /**
     * Lấy tên sản phẩm.
     * @return tên sản phẩm
     */
    public String getProduct() {
        return product;
    }

    /**
     * Lấy tên người mua.
     * @return tên tài khoản người mua
     */
    public String getBuyer() {
        return buyer;
    }

    /**
     * Lấy giá chốt đơn hàng.
     * @return giá chốt
     */
    public double getPrice() {
        return price;
    }

    /**
     * Lấy ngày hoàn thành giao dịch.
     * @return thời gian hoàn thành
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * Lấy trạng thái đơn hàng.
     * @return trạng thái (FINISHED, PAID, v.v.)
     */
    public String getStatus() {
        return status;
    }
}

