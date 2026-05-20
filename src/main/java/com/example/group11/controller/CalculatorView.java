package com.example.group11.controller;

import javafx.scene.control.Label;

import java.text.NumberFormat;
import java.util.Locale;

public class CalculatorView {

    // Cập nhật số lượng (Số nguyên) cho các Label như: Tổng sản phẩm, Lượt đấu giá.
    public static void updateCount(Label label, int value) {
        if (label != null) {
            label.setText("🎁 "+ NumberFormat.getIntegerInstance(Locale.US).format(value));
            label.setStyle("-fx-text-fill: #2196F3;");
        }
    }

    // Cập nhật tiền tệ (Currency) cho các Label như: Tổng doanh thu, Giá chốt.
    public static void updateCurrency(Label label, double value) {
        if (label != null) {
            String formatted = String.format("$%,.2f", value);
            label.setText(formatted);
            label.setStyle("-fx-text-fill: #FFC107; -fx-font-weight: bold;");
        }
    }

    // Phương thức chung để cập nhật trạng thái kèm màu sắc (Nếu bạn cần)
    public static void updateStatus(Label label, String status, String colorHex) {
        if (label != null) {
            label.setText(status.toUpperCase());
            label.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold;");
        }
    }
}
