package com.example.group11.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class NotificationController {

    //Hàm hiển thị thông báo thành công (Information)
    public static void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // Ẩn phần header mặc định để hộp thoại gọn gàng hơn
        alert.setContentText(content);
        alert.showAndWait(); // Hiển thị cửa sổ và dừng luồng cho đến khi người dùng bấm OK
    }

    // Hàm hiển thị thông báo Alert nhanh (Warning)
    public static void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /*
      CẬP NHẬT: Hàm hiển thị hộp thoại xác nhận hành động (Confirmation)
     */
    public static boolean showConfirmation(String title, String headerText, String contentText, String yesButtonText, String noButtonText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        // Định nghĩa 2 nút bấm tùy biến text theo ý muốn
        ButtonType buttonYes = new ButtonType(yesButtonText);
        ButtonType buttonNo = new ButtonType(noButtonText);
        alert.getButtonTypes().setAll(buttonYes, buttonNo);

        // Hiển thị và chờ người dùng phản hồi
        Optional<ButtonType> response = alert.showAndWait();

        // Trả về true nếu người dùng bấm trúng nút Đồng ý
        return response.isPresent() && response.get() == buttonYes;
    }
}
