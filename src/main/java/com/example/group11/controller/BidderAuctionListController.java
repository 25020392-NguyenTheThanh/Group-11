package com.example.group11.controller;

import com.auction.model.user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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

    private User user;

    public void setUser(User user) {
        this.user = user;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mặc định khi mở lên, Dashboard sẽ ở trạng thái Active
        setActiveStyle(btnDashboardS);

        // Xử lý cho MenuButton Trạng thái
        GenerationSupport.setupMenuButtonUpdate(auctionStatus);

        // Xử lý cho MenuButton Sản phẩm
        GenerationSupport.setupMenuButtonUpdate(auctionProduct);

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
}
