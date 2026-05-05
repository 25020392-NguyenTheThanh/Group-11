package com.example.group11.controller;

import com.auction.model.user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerAuctionListController implements Initializable {

    @FXML
    private Button SystemNotification;

    @FXML
    private Button addFundsBtn;

    @FXML
    private VBox auctionLive;

    @FXML
    private Button avatarBtn;

    @FXML
    private Button btnAnalytics;

    @FXML
    private Button btnMyListings;

    @FXML
    private Button btnSettings;

    @FXML
    private Button btnShipping;

    @FXML
    private GridPane contentGrid;

    @FXML
    private BorderPane mainPane;

    @FXML
    private TextField searchBar;

    @FXML
    private Button sellItemSidebar;

    @FXML
    private Label walletBalance;

    @FXML
    private VBox analyticsPane; // Vùng chứa biểu đồ và các báo cáo

    @FXML
    private LineChart<String, Number> revenueChart; // Ví dụ biểu đồ đường

    private User user;

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setActiveStyle(btnMyListings);
    }

    @FXML
    private void handleSwitchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String tabName = clickedButton.getText().trim().toUpperCase();

        // 1. Reset UI Sidebar
        resetAllButtons();
        setActiveStyle(clickedButton);

        // 2. Điều hướng hiển thị nội dung
        // Mặc định ẩn tất cả các vùng nội dung chính trước khi chọn
        contentGrid.setVisible(false);
        contentGrid.setManaged(false);

        analyticsPane.setVisible(false);
        analyticsPane.setManaged(false);


        // 3. Hiển thị đúng vùng nội dung dựa trên tab được nhấn
        switch (tabName) {
            case "ANALYTICS":
                analyticsPane.setVisible(true);
                analyticsPane.setManaged(true);
                //loadAnalyticsData(); // Gọi hàm đổ dữ liệu vào biểu đồ
                break;

            case "MY LISTINGS":
                contentGrid.setVisible(true);
                contentGrid.setManaged(true);
                if (auctionLive != null) {
                    auctionLive.setVisible(true);
                    auctionLive.setManaged(true);
                }
                break;

            case "SETTINGS":
                // Chỉ hiện các tùy chọn cài đặt (nếu có pane riêng)
                System.out.println("Đang mở cài đặt...");
                break;

            case "SHIPPING":
                // Hiện danh sách vận chuyển
                contentGrid.setVisible(true);
                contentGrid.setManaged(true);
                break;
        }
    }

    // Reset style cho toàn bộ các nút điều hướng của Seller
    private void resetAllButtons() {
        String inactiveStyle = "-fx-background-color: transparent; " +
                "-fx-text-fill: #94A3B8; " +
                "-fx-font-weight: bold; " +
                "-fx-border-width: 0;";

        btnMyListings.setStyle(inactiveStyle);
        btnAnalytics.setStyle(inactiveStyle);
        btnShipping.setStyle(inactiveStyle);
        btnSettings.setStyle(inactiveStyle);
    }

    // Làm nổi bật nút đang được chọn (Active)
    private void setActiveStyle(Button button) {
        String activeStyle = "-fx-background-color: #112240; " +
                "-fx-text-fill: #FFD700; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: transparent #FFD700 transparent transparent; " +
                "-fx-border-width: 0 4 0 0;";
        button.setStyle(activeStyle);
    }
}
