package com.example.group11.controller;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class EquilibriumAnimation {

    public static void startEquilibriumAnimation(Rectangle scaleBar, VBox boxContainer, VBox coinContainer) {
        Duration speed = Duration.seconds(1.8);

        // 1. Thanh cân bập bênh (Góc âm là nghiêng trái xuống)
        RotateTransition rotateBar = new RotateTransition(speed, scaleBar);
        rotateBar.setFromAngle(-10);
        rotateBar.setToAngle(10);
        rotateBar.setCycleCount(Animation.INDEFINITE);
        rotateBar.setAutoReverse(true);
        rotateBar.setInterpolator(Interpolator.EASE_BOTH);
        rotateBar.play();

        // 2. Khối Hộp (Bên trái) - Khớp thứ tự: Nghiêng trái xuống -> Hộp đi xuống
        TranslateTransition animBox = new TranslateTransition(speed, boxContainer);
        animBox.setFromY(20);  // Giảm biên độ xuống 20
        animBox.setToY(-20);
        animBox.setCycleCount(Animation.INDEFINITE);
        animBox.setAutoReverse(true);
        animBox.setInterpolator(Interpolator.EASE_BOTH);
        animBox.play();

        // 3. Khối Tiền (Bên phải) - Đảo ngược lại
        TranslateTransition animCoin = new TranslateTransition(speed, coinContainer);
        animCoin.setFromY(-35); // Giảm biên độ xuống 35 (vì thanh bên này dài hơn)
        animCoin.setToY(35);
        animCoin.setCycleCount(Animation.INDEFINITE);
        animCoin.setAutoReverse(true);
        animCoin.setInterpolator(Interpolator.EASE_BOTH);
        animCoin.play();
    }

    public static void playSwitchAnimation(Node leftNode, Node rightNode, Runnable onFinishedAction, HBox rootHBox) {
        double width = rootHBox.getWidth() / 2; // Sử dụng một nửa chiều rộng thực tế của root
        Duration duration = Duration.seconds(0.6);

        // 1. Hiệu ứng di chuyển trượt
        TranslateTransition moveLeft = new TranslateTransition(duration, leftNode);
        moveLeft.setByX(width);
        moveLeft.setInterpolator(Interpolator.EASE_BOTH); // Gia tốc mượt ở hai đầu

        TranslateTransition moveRight = new TranslateTransition(duration, rightNode);
        moveRight.setByX(-width);
        moveRight.setInterpolator(Interpolator.EASE_BOTH);

        // 2. Hiệu ứng mờ dần (Fade) giúp giảm cảm giác khựng khi tráo đổi Node
        FadeTransition fadeOutLeft = new FadeTransition(duration.divide(1.5), leftNode);
        fadeOutLeft.setFromValue(1.0);
        fadeOutLeft.setToValue(0.5);

        FadeTransition fadeOutRight = new FadeTransition(duration.divide(1.5), rightNode);
        fadeOutRight.setFromValue(1.0);
        fadeOutRight.setToValue(0.5);

        ParallelTransition pt = new ParallelTransition(moveLeft, moveRight, fadeOutLeft, fadeOutRight);

        pt.setOnFinished(e -> {
            // Reset lại các thuộc tính trước khi tráo đổi
            leftNode.setTranslateX(0);
            rightNode.setTranslateX(0);
            leftNode.setOpacity(1.0);
            rightNode.setOpacity(1.0);

            // Thực hiện thay đổi cấu trúc layout
            onFinishedAction.run();

            // Thêm một hiệu ứng Fade In nhẹ cho nội dung mới xuất hiện
            Node newNode = rootHBox.getChildren().get(0); // Lấy node mới ở bên trái
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.3), newNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        pt.play();
    }

    /*
     * Hàm dùng chung để mở tệp bằng ứng dụng mặc định của hệ thống
     */
    public static void openFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    System.out.println("Desktop is not supported on this platform.");
                }
            } else {
                System.out.println("File không tồn tại: " + filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
     * Hiệu ứng gạch chân khi di chuột (Hover)
     */
    public static void setupHoverEffect(Hyperlink link) {
        link.setOnMouseEntered(e -> link.setUnderline(true));
        link.setOnMouseExited(e -> link.setUnderline(false));
    }


}
