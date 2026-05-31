package com.auction.network;
import java.io.Serializable;
import java.util.List;

public class AdminPayload implements Serializable {
    public int    targetId;       // userId / auctionId / itemId tùy action
    public String reason;         // lý do ban / hủy phiên...
    public String newPassword;    // dùng cho ADMIN_RESET_USER_PASSWORD
    public java.util.List<Integer> targetIds; // Danh sách ID cho các tác vụ hàng loạt (batch)

    public AdminPayload() {}


    public AdminPayload(int targetId) {
        this.targetId = targetId;
    }
    public AdminPayload(List<Integer> targetIds) {
        this.targetIds = targetIds;
    }
    public AdminPayload (int targetId,String reason){
        this.targetId =targetId;
        this.reason = reason;
    }
}
