package com.auction.network;
import java.io.Serializable;
public class AdminPayload implements Serializable {
    public int    targetId;       // userId / auctionId / itemId tùy action
    public String reason;         // lý do ban / hủy phiên...
    public String newPassword;    // dùng cho ADMIN_RESET_USER_PASSWORD

    public AdminPayload() {}


    public AdminPayload(int targetId) {
        this.targetId = targetId;
    }
    public AdminPayload (int targetId,String reason){
        this.targetId =targetId;
        this.reason = reason;
    }
}
