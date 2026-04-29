package com.example.group11.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class auctionListController implements Initializable {
    @FXML
    private Circle Avarta;

    @FXML
    private Button SystemNotification;

    @FXML
    private Button allLIst;

    @FXML
    private MenuButton auctionProduct;

    @FXML
    private MenuButton auctionStatus;

    @FXML
    private VBox autionLive;

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
    private TextField searchBar;

    @FXML
    private Button sellItem;




    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mặc định khi mở lên, Dashboard sẽ ở trạng thái Active
        setActiveStyle(btnDashboardS);
    }

    @FXML
    private void handleSwitchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();

        // 1. Reset tất cả các nút về trạng thái bình thường
        resetAllButtons();

        // 2. Làm nổi bật nút được nhấn
        setActiveStyle(clickedButton);

        // 3. Thực hiện chuyển nội dung (Ví dụ mẫu)
        String tabName = clickedButton.getText().trim();
        System.out.println("Đang chuyển sang tab: " + tabName);

        // Tại đây bạn có thể dùng mainPane.setCenter(...) để đổi giao diện
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
