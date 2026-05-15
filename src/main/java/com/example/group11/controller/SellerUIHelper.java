package com.example.group11.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class SellerUIHelper {
    @FXML
    public static Button btnAnalytics;

    @FXML
    public static Button btnMyListings;

    @FXML
    public static Button btnOrderHistory;

    @FXML
    public static Button btnSettings;

    @FXML
    public static VBox myListingsView;

    @FXML
    public static VBox orderHistoryView;

    @FXML
    public static VBox registerProductView;

    @FXML
    public static VBox analyticsPane;

    // Hiển thị một Pane và ẩn tất cả các Pane khác trong StackPane
    public static void showView(VBox viewToShow, List<VBox> allViews) {
        if (allViews == null) return;
        for (VBox view : allViews) {
            if (view != null) {
                boolean isTarget = (view == viewToShow);
                view.setVisible(isTarget);
                view.setManaged(isTarget);
            }
        }
    }

    // Reset style cho toàn bộ các nút điều hướng của Seller
    public static void resetAllButtons(List<Button> allButtons) {
        if (allButtons == null) return;
        String inactiveStyle = "-fx-background-color: transparent; " +
                "-fx-text-fill: #94A3B8; " +
                "-fx-font-weight: bold; " +
                "-fx-border-width: 0;";

        for (Button btn : allButtons) {
            if (btn != null) {
                btn.setStyle(inactiveStyle);
            }
        }
    }

    // Làm nổi bật nút đang được chọn (Active)
    public static void setActiveStyle(Button button) {
        String activeStyle = "-fx-background-color: #112240; " +
                "-fx-text-fill: #FFD700; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: transparent #FFD700 transparent transparent; " +
                "-fx-border-width: 0 4 0 0;";
        button.setStyle(activeStyle);
    }

    // Hàm phụ để lấy VBox tương ứng với Button ID
    public static VBox getVBoxFromId(String id, Map<String, VBox> viewMap) {
        if (viewMap == null) return null;
        return viewMap.get(id);
    }

    // Hàm phụ để tìm xem nút nào đang được Active dựa trên Style
    public static Button findActiveButton(List<Button> allButtons) {
        if (allButtons == null) return null;
        for (Button btn : allButtons) {
            if (btn != null && btn.getStyle() != null && btn.getStyle().contains("#FFD700")) {
                return btn;
            }
        }
        return null;
    }


}
