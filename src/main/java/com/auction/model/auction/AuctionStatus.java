package com.auction.model.auction;

public enum AuctionStatus {
    OPEN , RUNNING , FINISHED , PAID , CANCELED ;
    // OPEN — phiên vừa được tạo, chưa bắt đầu nhận giá
    // RUNNING — đang diễn ra, người dùng có thể đặt giá
    // FINISHED — hết giờ, đã xác định người thắng
    // PAID — người thắng đã thanh toán xong
    // CANCELED — phiên bị hủy (không có ai đặt giá, hoặc admin hủy)
}
