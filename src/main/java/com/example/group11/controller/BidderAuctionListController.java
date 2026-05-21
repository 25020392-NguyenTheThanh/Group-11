package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class BidderAuctionListController implements Initializable {

    @FXML
    private Button SystemNotification;

    @FXML
    private Button addFundsBtn;

    @FXML
    private Button allList;

    @FXML
    private VBox auctionLive;

    @FXML
    private MenuButton auctionProduct;

    @FXML
    private MenuButton auctionStatus;

    @FXML
    private Button avatarBtn;

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
    private Label totalAuctionsLabel;

    @FXML
    private Label walletBalance;

    @FXML
    private VBox profileDropdown;

    @FXML
    private VBox notificationDropdown;

    @FXML
    private VBox notificationListContainer;

    private User user;

    public void setUser(User user) {
        this.user = user;
        if (user instanceof Bidder bidder) {
            walletBalance.setText(String.format("%.2f", bidder.getBalance()));
        }
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


    //  Tự động cập nhật nhãn (Text) của MenuButton khi người dùng chọn một Item bên trong
    private void setupMenuButtonUpdate(MenuButton menuButton) {
        for (MenuItem item : menuButton.getItems()) {
            item.setOnAction(event -> {
                menuButton.setText(item.getText().toUpperCase());
                // Gọi logic lọc dữ liệu tại đây
                System.out.println("Bidder đang lọc theo: " + item.getText());
            });
        }
    }

    @FXML
    void handleSwitchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String tabName = clickedButton.getText().trim().toUpperCase();

        // 1. Reset màu sắc của tất cả nút Sidebar và highlight nút vừa bấm
        resetAllButtons();
        setActiveStyle(clickedButton);

        // 2. Xử lý hiển thị Header và ContentGrid tùy theo Tab
        if (tabName.equals("SETTINGS")) {
            headerSection.setVisible(false);
            headerSection.setManaged(false);

            contentGrid.setVisible(false);
            contentGrid.setManaged(false);
        } else {
            headerSection.setVisible(true);
            headerSection.setManaged(true);

            contentGrid.setVisible(true);
            contentGrid.setManaged(true);

            // Cập nhật số lượng phiên đấu giá tương ứng (logic mẫu)
            // totalAuctionsLabel.setText("10");
        }

        System.out.println("Bidder đang xem tab: " + tabName);
    }

    //Đưa tất cả các nút về trạng thái bình thường
    private void resetAllButtons() {
        String inactiveStyle = "-fx-background-color: transparent; " +
                "-fx-text-fill: #94A3B8; " +
                "-fx-font-weight: bold; " +
                "-fx-border-width: 0;";

        btnDashboardS.setStyle(inactiveStyle);
        btnMyBids.setStyle(inactiveStyle);
        btnWatchlist.setStyle(inactiveStyle);
        btnHistory.setStyle(inactiveStyle);
        btnSettings.setStyle(inactiveStyle);
    }


    // Làm nổi bật nút đang được chọn (Active)

    private void setActiveStyle(Button button) {
        String activeStyle = "-fx-background-color: #112240; " +
                "-fx-text-fill: #FFD700; " + // Màu vàng Gold
                "-fx-font-weight: bold; " +
                "-fx-border-color: transparent #FFD700 transparent transparent; " +
                "-fx-border-width: 0 4 0 0;";
        button.setStyle(activeStyle);
    }

    @FXML
    private void toggleProfileDropdown(ActionEvent event) {
        if (notificationDropdown.isVisible()) {
            notificationDropdown.setVisible(false);
            notificationDropdown.setManaged(false);
        }

        boolean isVisible = profileDropdown.isVisible();
        profileDropdown.setVisible(!isVisible);
        profileDropdown.setManaged(!isVisible);
    }

    @FXML
    private void handleShowNotifications(ActionEvent event) {
        if (profileDropdown != null && profileDropdown.isVisible()) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }

        boolean isCurrentlyVisible = notificationDropdown.isVisible();
        notificationDropdown.setVisible(!isCurrentlyVisible);
        notificationDropdown.setManaged(!isCurrentlyVisible);
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        System.out.println("Chuyển hướng đến trang Thông tin cá nhân...");
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        boolean confirm = NotificationController.showConfirmation(
                "Xác nhận đăng xuất",
                "Bạn có chắc chắn muốn đăng xuất không?",
                "Hệ thống sẽ kết thúc phiên làm việc hiện tại của bạn.",
                "Có, Đăng xuất",
                "Không, Ở lại"
        );
        if (confirm) {
            try {
                System.out.println("Đang thực hiện gửi yêu cầu đăng xuất lên Server...");
                ServerConnection.getInstance().stopListening();
                ServerConnection.getInstance().disconnect();
                FXMLLoader loader = GenerationSupport.changeScene(event, "login-view.fxml", "Đăng nhập");

                if (loader != null) {
                    user = null;
                } else {
                    System.err.println("Lỗi: Không thể tải giao diện login-view.fxml");
                }
            } catch (Exception e) {
                System.err.println("Lỗi kết nối khi đăng xuất: " + e.getMessage());
                NotificationController.showError("Lỗi kết nối", "Không thể kết nối tới Server để đăng xuất.");
            }
        }
    }
}
