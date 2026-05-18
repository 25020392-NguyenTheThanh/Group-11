package com.auction.client;

import com.auction.model.user.User;
import com.auction.network.*;

public class TestConnection {
    public static void main(String[] args) throws Exception {

        // --- Test 1: Kết nối ---
        System.out.println("=== Test 1: Kết nối server ===");
        ServerConnection conn = ServerConnection.getInstance();
        conn.connect();
        System.out.println("✓ Kết nối thành công");

        // --- Test 2: Register ---
        System.out.println("\n=== Test 2: Đăng ký tài khoản ===");
        RegisterPayload reg = new RegisterPayload();
        reg.username = "bidder01";
        reg.password = "123456";
        reg.email    = "bidder01@test.com";
        reg.role     = "BIDDER";

        Response regResponse = conn.send(RequestType.REGISTER, reg);
        System.out.println("Register success: " + regResponse.isSuccess());
        System.out.println("Message: "          + regResponse.getMessage());

        // --- Test 3: Login đúng ---
        System.out.println("\n=== Test 3: Login đúng ===");
        LoginPayload login = new LoginPayload("bidder01" , "123456");

        Response loginResponse = conn.send(RequestType.LOGIN, login);
        System.out.println("Login success: " + loginResponse.isSuccess());
        if (loginResponse.isSuccess()) {
            User user = (User) loginResponse.getData();
            System.out.println("✓ User: " + user.getUsername() + " | Role: " + user.getRole());
        }

        // --- Test 4: Login sai mật khẩu ---
        System.out.println("\n=== Test 4: Login sai mật khẩu ===");
        login.password = "saimk";
        Response failResponse = conn.send(RequestType.LOGIN, login);
        System.out.println("Login success: " + failResponse.isSuccess()); // phải = false
        System.out.println("Message: "       + failResponse.getMessage()); // phải hiện lỗi

        conn.disconnect();
        System.out.println("\n=== Tất cả test hoàn thành ===");
    }
}
