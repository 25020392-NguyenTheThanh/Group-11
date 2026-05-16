package com.example.group11.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.Optional;

public class ImagesController {

    // Hiển thị ảnh lên giao diện và ẩn thông báo hướng dẫn.
    public static void displayImage(File file, ImageView productImageView, VBox uploadPrompt) {
        if (file == null) return;

        Image image = new Image(file.toURI().toString());
        productImageView.setImage(image);

        // Hiện ImageView và ẩn VBox hướng dẫn
        productImageView.setVisible(true);
        uploadPrompt.setVisible(false);

        System.out.println("Đã chọn ảnh: " + file.getAbsolutePath());
    }

    // Xóa ảnh - Trả về null nếu người dùng đồng ý xóa, trả về file cũ nếu hủy bỏ
    public static File confirmRemoveImage(File currentFile, ImageView productImageView, VBox uploadPrompt) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có muốn xóa ảnh hiện tại để chọn lại không?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        Optional<ButtonType> response = alert.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.YES) {
            // Thực hiện reset giao diện
            productImageView.setImage(null);
            productImageView.setVisible(false);
            uploadPrompt.setVisible(true);

            System.out.println("Đã xóa ảnh trên giao diện.");
            return null; // Trả về null để Controller chính cập nhật lại biến lưu file
        }

        return currentFile; // Người dùng không xóa, giữ nguyên file cũ
    }
}
