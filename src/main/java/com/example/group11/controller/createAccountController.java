package com.example.group11.controller;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;


public class createAccountController implements Initializable {

    @FXML
    private VBox Admin;

    @FXML
    private VBox Bidder;

    @FXML
    private VBox Seller;

    private VBox lastSelected;

    // Các chuỗi Style cơ bản (không bao gồm Scale vì sẽ xử lý bằng Animation)
    private final String NORMAL_STYLE =
            "-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #e2e8f0; " +
                    "-fx-border-radius: 15; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 5);";

    private final String SELECTED_STYLE =
            "-fx-background-color: #131b2e; -fx-background-radius: 15; -fx-border-color: #0058be; " +
                    "-fx-border-width: 2; -fx-border-radius: 15; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,88,190,0.3), 20, 0, 0, 10);";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mặc định chọn Admin
        applySelection(Admin, false); // false để không chạy animation khi vừa mở app
    }

    @FXML
    private void handleSelection(MouseEvent event) {
        VBox source = (VBox) event.getSource();
        if (source != lastSelected) {
            applySelection(source, true);
        }
    }

    private void applySelection(VBox target, boolean animate) {
        // 1. Thu nhỏ cái cũ về bình thường
        if (lastSelected != null) {
            playTransition(lastSelected, 1.0, animate);
            updateContentColor(lastSelected, false);
            lastSelected.setStyle(NORMAL_STYLE);
            lastSelected.setViewOrder(0);
        }

        // 2. Phóng to cái mới
        playTransition(target, 1.05, animate);
        updateContentColor(target, true);
        target.setStyle(SELECTED_STYLE);
        target.setViewOrder(-1.0); // Luôn nằm trên cùng khi phóng to

        lastSelected = target;
    }

    // Hàm tạo hiệu ứng co giãn mượt mà
    private void playTransition(VBox box, double scaleValue, boolean animate) {
        if (!animate) {
            box.setScaleX(scaleValue);
            box.setScaleY(scaleValue);
            return;
        }
        ScaleTransition st = new ScaleTransition(Duration.millis(250), box);
        st.setToX(scaleValue);
        st.setToY(scaleValue);
        st.play();
    }

    private void updateContentColor(VBox card, boolean isSelected) {
        String textColor = isSelected ? "white" : "#131b2e";
        String subTextColor = isSelected ? "#94a3b8" : "#64748b";

        for (Node node : card.getChildren()) {
            if (node instanceof Label) {
                Label lbl = (Label) node;
                if (lbl.getStyle().contains("font-size: 28px")) continue;

                boolean isTitle = lbl.getStyle().contains("font-weight: bold");
                lbl.setStyle(lbl.getStyle() + "; -fx-text-fill: " + (isTitle ? textColor : subTextColor) + ";");
            }
            if (node instanceof Button) {
                Button btn = (Button) node;
                btn.setStyle(isSelected ?
                        "-fx-background-color: #0058be; -fx-text-fill: white; -fx-border-radius: 8;" :
                        "-fx-background-color: transparent; -fx-border-color: #131b2e; -fx-border-radius: 8; -fx-text-fill: #131b2e;");
            }
        }
    }
}
