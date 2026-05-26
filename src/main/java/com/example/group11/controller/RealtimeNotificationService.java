package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.network.Notification;
import java.util.function.Consumer;

public class RealtimeNotificationService {

    public static void setupRealtimeNotifications(Consumer<Notification> listener) {
        ServerConnection.getInstance().addNotificationHandler(listener);
    }

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
