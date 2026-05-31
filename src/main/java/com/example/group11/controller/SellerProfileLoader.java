package com.example.group11.controller;

import com.auction.model.user.User;
import javafx.scene.layout.VBox;

/**
 * Trình nạp thông tin cá nhân của Người bán (Seller Profile Loader).
 * Lớp này chịu trách nhiệm nạp dữ liệu hồ sơ cá nhân của Người bán vào trong giao diện VBox.
 * Lớp này được thiết kế như một Utility Class, không có hàm khởi tạo và chỉ chứa các phương thức tĩnh.
 */
public class SellerProfileLoader {

    /**
     * Nạp dữ liệu thông tin cá nhân của Seller vào profileView.
     * 
     * @param user Đối tượng người dùng hiện tại (phải thuộc vai trò Seller)
     * @param profileView VBox hiển thị vùng thông tin cá nhân trên giao diện
     */
    public static void loadProfileData(User user, VBox profileView) {
        if (user == null || profileView == null) return;
        profileView.getChildren().clear();
        VBox builtProfile = ProfileViewFactory.create(user, msg ->
            NotificationController.showNotification("Đổi mật khẩu", msg)
        );
        profileView.getChildren().add(builtProfile);
    }
}
