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
        boolean isConfirmed = NotificationController.showConfirmation(
                "Xác nhận gỡ ảnh",
                "Bạn có muốn gỡ bỏ hình ảnh hiện tại không?",
                "Tệp ảnh này sẽ bị xóa hoàn toàn khỏi bộ nhớ tạm của hệ thống.",
                "Đồng ý",
                "Hủy bỏ"
        );

        if (isConfirmed) {
            // 1. Gỡ liên kết ảnh trên UI để mở khóa File (Tránh lỗi File Lock trên Windows)
            productImageView.setImage(null);
            productImageView.setVisible(false);
            uploadPrompt.setVisible(true);

            // Ép Java giải phóng tài nguyên đang giữ luồng đọc ảnh ngay lập tức
            System.gc();

            // 2. Thực hiện xóa tận gốc file vật lý trong package
            if (currentFile != null && currentFile.exists()) {
                boolean isDeleted = currentFile.delete();
                if (isDeleted) {
                    System.out.println("Đã xóa file ảnh vật lý thành công tại: " + currentFile.getAbsolutePath());
                } else {
                    System.err.println("Không thể xóa file vật lý! Tệp tin vẫn đang bị lock bởi hệ thống.");
                }
            }

            return null; // Trả về null để Controller cập nhật lại biến selectedImageFile
        }

        return currentFile; // Người dùng nhấn "Hủy bỏ", giữ nguyên trạng thái cũ
    }
}
