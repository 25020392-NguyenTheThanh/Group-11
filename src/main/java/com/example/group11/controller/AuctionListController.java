package com.example.group11.controller;

import com.auction.model.user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {
    @FXML
    private Circle Avatar;

    @FXML
    private Button SystemNotification;

    @FXML
    private Button allList;

    @FXML
    private VBox auctionLive;

    @FXML
    private MenuButton auctionProduct;

    @FXML
    private MenuButton auctionStatus;

    @FXML
    private Button btnDashboardS;

    @FXML
    private Button btnHistory;

    @FXML
    private Button btnMyBids;

    @FXML
    private Button btnSettings;

    @FXML
    private Button btnWatchlist;

    @FXML
    private GridPane contentGrid;

    @FXML
    private VBox headerSection;

    @FXML
    private BorderPane mainPane;

    @FXML
    private TextField searchBar;

    @FXML
    private Button sellItemSidebar;

    @FXML
    private Label totalAuctionsLabel;

    private User user;

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mặc định khi mở lên, Dashboard sẽ ở trạng thái Active
        setActiveStyle(btnDashboardS);

        // Xử lý cho MenuButton Trạng thái
        setupMenuButtonUpdate(auctionStatus);

        // Xử lý cho MenuButton Sản phẩm
        setupMenuButtonUpdate(auctionProduct);
    }

    /**
     * Hàm tiện ích để tự động cập nhật text của MenuButton khi chọn MenuItem
     */
    private void setupMenuButtonUpdate(MenuButton menuButton) {
        for (MenuItem item : menuButton.getItems()) {
            item.setOnAction(event -> {
                // Cập nhật text của MenuButton thành text của MenuItem vừa chọn
                menuButton.setText(item.getText().toUpperCase());

                // Tại đây bạn có thể gọi hàm để lọc dữ liệu (ví dụ: filterAuctions())
                System.out.println("Đang lọc theo: " + item.getText());
            });
        }
    }

    @FXML
    private void handleSwitchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String tabName = clickedButton.getText().trim().toUpperCase();

        // 1. Reset và làm nổi bật nút (UI Sidebar)
        resetAllButtons();
        setActiveStyle(clickedButton);

        // 2. Xử lý logic Header và Số lượng tự động
        if (tabName.equals("SETTINGS")) {
            // 1. Ẩn tiêu đề "Tổng các phiên" khi vào Settings
            headerSection.setVisible(false);
            headerSection.setManaged(false);

            // 2. XÓA/ẨN hiển thị sản phẩm
            contentGrid.setVisible(false);
            contentGrid.setManaged(false);
        } else {
            // Hiện lại tiêu đề cho các tab khác
            headerSection.setVisible(true);
            headerSection.setManaged(true);

//            // Logic cập nhật số lượng tự động dựa trên Tab
//            int count = getAuctionCount(tabName);
//            totalAuctionsLabel.setText(String.valueOf(count));
        }

        // 3. Thực hiện chuyển nội dung thực tế (Ví dụ load FXML khác vào Center)
        System.out.println("Đang hiển thị nội dung cho: " + tabName);
        // switchCenterContent(tabName);
    }

    private void resetAllButtons() {
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-border-width: 0;";
        btnDashboardS.setStyle(inactiveStyle);
        btnMyBids.setStyle(inactiveStyle);
        btnWatchlist.setStyle(inactiveStyle);
        btnHistory.setStyle(inactiveStyle);
        btnSettings.setStyle(inactiveStyle);
    }

    private void setActiveStyle(Button button) {
        String activeStyle = "-fx-background-color: #112240; " +
                "-fx-text-fill: #FFD700; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: transparent #FFD700 transparent transparent; " +
                "-fx-border-width: 0 4 0 0;";
        button.setStyle(activeStyle);
    }
}
