package com.example.group11.controller;

import com.auction.model.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerAuctionListController implements Initializable {
    @FXML
    private Button SystemNotification;

    @FXML
    private Button addFundsBtn;

    @FXML
    private VBox analyticsPane;

    @FXML
    private VBox auctionLive;

    @FXML
    private Circle avartaSeller;

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
    private TextField filterSearchId;

    @FXML
    private ComboBox<String> filterSort;

    @FXML
    private ComboBox<String> filterStatus;

    @FXML
    private BorderPane mainPane;

    @FXML
    private VBox myListingsView;

    @FXML
    private LineChart<?, ?> revenueChart;

    @FXML
    private TextField searchBar;

    @FXML
    private Button sellItemSidebar;

    @FXML
    private StackPane totalProducts;

    @FXML
    private Label totalProductsLabel;

    @FXML
    private Label walletBalance;

//    @FXML
//    private VBox analyticsPane; // Vùng chứa biểu đồ và các báo cáo
//
//    @FXML
//    private LineChart<String, Number> revenueChart; // Ví dụ biểu đồ đường

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

        // Dùng ID của button để phân biệt (chính xác hơn dùng Text)
        String buttonId = clickedButton.getId();

        // 1. Cập nhật UI Sidebar
        resetAllButtons();
        setActiveStyle(clickedButton);

        // 2. Điều hướng nội dung
        switch (buttonId) {
            case "btnMyListings":
                showView(myListingsView);
                break;

            case "btnAnalytics":
                showView(analyticsPane);
                // loadAnalyticsData(); // Hàm để vẽ biểu đồ
                break;

            case "btnShipping":
                // Nếu bạn có shippingPane thì show ở đây
                // Tạm thời ẩn hết hoặc hiện thông báo
                hideAllViews();
                System.out.println("Tab Shipping đang được phát triển");
                break;

            case "btnSettings":
                hideAllViews();
                System.out.println("Tab Settings đang được phát triển");
                break;
        }
    }

//    private void setupFilters() {
//        // Thiết lập trạng thái theo đúng yêu cầu của bạn
//        ObservableList<String> statuses = FXCollections.observableArrayList(
//                "Tất cả", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED"
//        );
//        filterStatus.setItems(statuses);
//        filterStatus.getSelectionModel().selectFirst(); // Mặc định chọn "Tất cả"
//
//        // Thiết lập sắp xếp
//        filterSort.setItems(FXCollections.observableArrayList(
//                "Mới nhất", "Giá cao đến thấp", "Giá thấp đến cao", "Gần kết thúc"
//        ));
//        filterSort.getSelectionModel().selectFirst();
//
//        // Lắng nghe sự kiện thay đổi filter
//        filterStatus.setOnAction(e -> handleFilterChange());
//    }
//
//    private void handleFilterChange() {
//        String selectedStatus = filterStatus.getValue();
//        System.out.println("Đang lọc theo trạng thái: " + selectedStatus);
//        // Tại đây gọi hàm để cập nhật lại contentGrid dựa trên database
//    }

    private void initComboBoxes() {
        // Thiết lập các trạng thái theo yêu cầu
        filterStatus.setItems(FXCollections.observableArrayList(
                "Tất cả trạng thái", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED"
        ));
        filterStatus.getSelectionModel().selectFirst();

        // Thiết lập các tùy chọn sắp xếp
        filterSort.setItems(FXCollections.observableArrayList(
                "Mới nhất", "Giá: Thấp đến Cao", "Giá: Cao đến Thấp", "Kết thúc sớm nhất"
        ));
        filterSort.getSelectionModel().selectFirst();
    }

    @FXML
    void handleSellItem(ActionEvent event) {
        FXMLLoader loader = EquilibriumAnimation.changeScene(event, "registerProduct-view.fxml", "Đăng ký sản phẩm");

        if (loader != null) {
            RegisterProductController controller = loader.getController();
        }
    }

    // Hiển thị một Pane và ẩn tất cả các Pane khác trong StackPane

    private void showView(VBox viewToShow) {
        hideAllViews();
        viewToShow.setVisible(true);
        viewToShow.setManaged(true);
    }

    private void hideAllViews() {
        myListingsView.setVisible(false);
        myListingsView.setManaged(false);
        analyticsPane.setVisible(false);
        analyticsPane.setManaged(false);
        // Thêm các Pane khác vào đây nếu có (shippingPane, settingsPane...)
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
