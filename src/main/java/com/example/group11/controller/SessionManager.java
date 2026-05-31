package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.network.RequestType;
import com.auction.model.user.User;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;

public class SessionManager {

    public static void logout(ActionEvent event, User user) throws Exception {
        System.out.println("Đang thực hiện gửi yêu cầu đăng xuất lên Server...");
        LiveAuctionController.clearEnteredAuctions();
        try {
            ServerConnection.getInstance().send(RequestType.LOGOUT, null);
        } catch (Exception ignored) {}
        ServerConnection.getInstance().stopListening();
        ServerConnection.getInstance().disconnect();
        FXMLLoader loader = GenerationSupport.changeScene(event, "login-view.fxml", "Đăng nhập");
        if (loader == null) {
            System.err.println("Lỗi: Không thể tải giao diện login-view.fxml");
        }
    }
}
