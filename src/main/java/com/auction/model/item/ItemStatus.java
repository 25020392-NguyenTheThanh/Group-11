package com.auction.model.item;

public enum ItemStatus {
    PENDING,     // đang chờ duyệt
    AVAILABLE,   // đang có sẵn, chưa đấu giá
    IN_AUCTION,  // đang được đấu giá
    SOLD,        // đã bán xong
    UNSOLD       // hết phiên nhưng không ai mua
}
