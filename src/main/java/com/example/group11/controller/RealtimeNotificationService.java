package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.network.Notification;
import java.util.function.Consumer;

/**
 * Dịch vụ thông báo thời gian thực (Real-time Notification Service).
 * Hỗ trợ các controller đăng ký lắng nghe thông báo đẩy từ máy chủ và định dạng
 * hiển thị chuỗi ký tự thông báo thân thiện với người dùng dựa trên loại thông báo.
 */
public class RealtimeNotificationService {

    /**
     * Đăng ký bộ xử lý (Callback listener) nhận thông báo thời gian thực từ Server.
     *
     * @param listener Callback nhận đối tượng Notification khi máy chủ đẩy thông báo xuống
     */
    public static void setupRealtimeNotifications(Consumer<Notification> listener) {
        ServerConnection.getInstance().addNotificationHandler(listener);
    }

    /**
     * Dịch và định dạng chuỗi ký tự hiển thị thân thiện (có emoji gợi nhớ) theo từng loại sự kiện thông báo.
     *
     * @param notification Đối tượng thông báo nhận được từ Server
     * @return Chuỗi ký tự thân thiện hiển thị trực tiếp lên giao diện người dùng
     */
    public static String getFriendlyMessage(Notification notification) {
        String type = notification.getType();
        Object data = notification.getData();
        String text = data != null ? data.toString() : "";

        return switch (type) {
            case "OUTBID" -> "⚠️ Bạn đã bị vượt giá! " + text;
            case "BID_SUCCESS" -> "✅ " + text;
            case "PAYMENT_SUCCESS" -> "💰 " + text;
            case "ENDING_SOON" -> "⏳ " + text;
            case "WATCHLIST_STARTED" -> "▶️ " + text;
            case "AUCTION_WON" -> "🏆 Chúc mừng! " + text;
            case "AUCTION_LOST" -> "❌ " + text;
            case "PRODUCT_APPROVED" -> "✔️ " + text;
            case "AUCTION_CREATED" -> "📅 " + text;
            case "NEW_AUCTION" -> "🆕 " + text;
            default -> "[" + type + "] " + text;
        };
    }
}
