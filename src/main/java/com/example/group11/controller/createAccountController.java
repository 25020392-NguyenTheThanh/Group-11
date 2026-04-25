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

    @FXML private VBox Admin, Bidder, Seller;
    private VBox lastSelected;

    private final String ACCENT_BLUE = "#0058be"; // Màu xanh khi nút được chọn

    // Styles của VBox (khối) giữ nguyên
    private final String NORMAL_STYLE =
            "-fx-background-color: white; -fx-background-radius: 25; -fx-border-color: #e2e8f0; -fx-border-radius: 25; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5);";

    private final String SELECTED_STYLE =
            "-fx-background-color: #131b2e; -fx-background-radius: 25; -fx-border-color: #131b2e; -fx-border-radius: 25; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(19,27,46,0.2), 20, 0, 0, 10);";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applySelection(Admin, false); // Mặc định chọn Admin
    }

    @FXML
    private void handleSelection(MouseEvent event) {
        VBox source = (VBox) event.getSource();
        if (source != lastSelected) {
            applySelection(source, true);
        }
    }

    private void applySelection(VBox target, boolean animate) {
        if (lastSelected != null) {
            playTransition(lastSelected, 1.0, animate);
            updateContentUI(lastSelected, false);
            lastSelected.setStyle(NORMAL_STYLE);
        }

        playTransition(target, 1.05, animate);
        updateContentUI(target, true);
        target.setStyle(SELECTED_STYLE);

        lastSelected = target;
    }

    private void playTransition(VBox box, double scaleValue, boolean animate) {
        if (!animate) {
            box.setScaleX(scaleValue);
            box.setScaleY(scaleValue);
            return;
        }
        ScaleTransition st = new ScaleTransition(Duration.millis(200), box);
        st.setToX(scaleValue);
        st.setToY(scaleValue);
        st.play();
    }

    private void updateContentUI(VBox card, boolean isSelected) {
        String iconColor = isSelected ? "white" : "#131b2e";

        for (Node node : card.getChildren()) {
            // 1. Icon
            if (node instanceof Label && node.getStyle().contains("-fx-font-size: 50px")) {
                node.setStyle("-fx-font-size: 50px; -fx-text-fill: " + iconColor + ";");
            }

            // 2. Logic hoán đổi Title/Description lồng trong VBox
            if (node instanceof VBox) {
                VBox textContainer = (VBox) node;
                for (Node subNode : textContainer.getChildren()) {
                    if (subNode instanceof Label) {
                        Label lbl = (Label) subNode;
                        if (lbl.getStyle().contains("font-weight: bold")) {
                            lbl.setVisible(!isSelected);
                            lbl.setManaged(!isSelected);
                        } else {
                            lbl.setVisible(isSelected);
                            lbl.setManaged(isSelected);
                            lbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
                        }
                    }
                }
            }

            // 3. Button - Đã sửa để bo góc đồng bộ
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (isSelected) {
                    // Khi Card được chọn: Nút có nền xanh, chữ trắng, không viền, bo góc 20
                    btn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
                } else {
                    // Khi Card không được chọn: Trả về style mặc định như FXML (nền trong, viền đen, bo góc 20)
                    btn.setStyle("-fx-background-color: transparent; -fx-border-color: #131b2e; -fx-border-width: 1.5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-text-fill: #131b2e; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
                }
            }
        }
    }
}