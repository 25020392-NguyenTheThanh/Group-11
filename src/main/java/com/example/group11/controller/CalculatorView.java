package com.example.group11.controller;

import javafx.scene.control.Label;

import java.text.NumberFormat;
import java.util.Locale;

public class CalculatorView {

    /**
     * Cập nhật số lượng (Số nguyên) cho các Label như: Tổng sản phẩm, Lượt đấu giá.
     *
     * @param label Nhãn Label cần hiển thị số lượng
     * @param value Giá trị số lượng cần hiển thị
     */
    public static void updateCount(Label label, int value) {
        if (label != null) {
            label.setText("🎁 "+ NumberFormat.getIntegerInstance(Locale.US).format(value));
            label.setStyle("-fx-text-fill: #2196F3;");
        }
    }

    /**
     * Cập nhật tiền tệ (Currency) cho các Label như: Tổng doanh thu, Giá chốt.
     *
     * @param label Nhãn Label hiển thị tiền tệ
     * @param value Giá trị số tiền cần định dạng và hiển thị
     */
    public static void updateCurrency(Label label, double value) {
        if (label != null) {
            String formatted = String.format("$%,.2f", value);
            label.setText(formatted);
            label.setStyle("-fx-text-fill: #FFC107; -fx-font-weight: bold;");
        }
    }

    /**
     * Phương thức chung để cập nhật trạng thái kèm màu sắc.
     *
     * @param label Nhãn Label hiển thị trạng thái
     * @param status Chuỗi trạng thái cần hiển thị
     * @param colorHex Mã màu Hex để đổi màu chữ cho nhãn
     */
    public static void updateStatus(Label label, String status, String colorHex) {
        if (label != null) {
            label.setText(status.toUpperCase());
            label.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold;");
        }
    }
}
