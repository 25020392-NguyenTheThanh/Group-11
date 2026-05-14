package com.example.group11.controller;

import com.auction.model.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
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
    private Button addImageButton;

    @FXML
    private Button addItemSidebar;

    @FXML
    private VBox analyticsPane;

    @FXML
    private Circle avartaSeller;

    @FXML
    private Button avatarBtn;

    @FXML
    private Button btnAnalytics;

    @FXML
    private Button btnLogout;

    @FXML
    private Button btnMyListings;

    @FXML
    private Button btnProfileInfo;

    @FXML
    private Button btnSettings;

    @FXML
    private Button btnOrderHistory;

    @FXML
    private MenuButton categoryMenuButton;

    @FXML
    private TableColumn<?, ?> colBuyer;

    @FXML
    private TableColumn<?, ?> colDate;

    @FXML
    private TableColumn<?, ?> colOrderId;

    @FXML
    private TableColumn<?, ?> colPrice;

    @FXML
    private TableColumn<?, ?> colProduct;

    @FXML
    private TableColumn<?, ?> colStatus;

    @FXML
    private GridPane contentGrid;

    @FXML
    private TextField depositField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField filterSearchId;

    @FXML
    private MenuButton filterSort;

    @FXML
    private MenuButton filterStatus;

    @FXML
    private BorderPane mainPane;

    @FXML
    private TextField minimumBidIncrementField;

    @FXML
    private VBox myListingsView;

    @FXML
    private VBox notificationDropdown;

    @FXML
    private VBox orderHistoryView;

    @FXML
    private TableView<?> orderTable;

    @FXML
    private TextField productNameField;

    @FXML
    private VBox profileDropdown;

    @FXML
    private VBox registerProductView;

    @FXML
    private LineChart<String, Number> revenueChart;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField startingPriceField;

    @FXML
    private Button submitButton;

    @FXML
    private Label totalProductsLabel;

    @FXML
    private Label totalBidsLabel;

    @FXML
    private Label soldProductsLabel;

    @FXML
    private Label totalRevenueLabel;

    @FXML
    private Label walletBalance;

    private User user;

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setActiveStyle(btnMyListings);
        showView(myListingsView);
        loadMyListingView();

        EquilibriumAnimation.setupMenuButtonUpdate(filterSort);
        EquilibriumAnimation.setupMenuButtonUpdate(filterStatus);
        EquilibriumAnimation.setupMenuButtonUpdate(categoryMenuButton);

        initRegistrationLogic();
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
                loadMyListingView();
                break;

            case "btnAnalytics":
                showView(analyticsPane);
                loadAnalyticsData(); // Hàm để vẽ biểu đồ
                break;

            case "btnOrderHistory":
                showView(orderHistoryView);
                System.out.println("Tab Shipping đang được phát triển");
                break;

            case "btnSettings":
                hideAllViews();
                System.out.println("Tab Settings đang được phát triển");
                break;
        }
    }

    @FXML
    void handleAddItem(ActionEvent event) {
        // Thay vì chuyển Scene, ta chuyển View trong cùng 1 cửa sổ
        resetAllButtons(); // Bỏ active ở các nút sidebar
        showView(registerProductView);
    }

    @FXML
    private void handleBackToListings(ActionEvent event) {
        // Quay lại trang danh sách
        setActiveStyle(btnMyListings);
        showView(myListingsView);
    }

    private void initRegistrationLogic() {

        // Xử lý nút xác nhận đăng ký
        submitButton.setOnAction(e -> {
            System.out.println("Đang xử lý đăng ký sản phẩm: " + productNameField.getText());
            // TODO: Triển khai logic lưu dữ liệu vào database
        });

        // Xử lý nút thêm ảnh
        addImageButton.setOnAction(e -> {
            System.out.println("Mở FileChooser để chọn ảnh...");
        });
    }

    private void loadMyListingView () {
        // TODO: LOGIC DATABASE
        int count = 12500;

        EquilibriumAnimation.updateCount(totalProductsLabel, count);
    }


    // TÍNH NĂNG: Load dữ liệu biểu đồ
    private void loadAnalyticsData() {
        // TODO: LOGIC DATABASE
        // 1. Viết Query lấy doanh thu theo tháng (Sum giá chốt của các đơn hàng Status=PAID)
        // 2. Loop kết quả và add vào Series

        // 1. Xóa dữ liệu cũ
        revenueChart.getData().clear();

        // 2. Tạo 4 Series đại diện cho 4 đường biểu đồ
        XYChart.Series<String, Number> totalSeries = new XYChart.Series<>();
        totalSeries.setName("Tổng doanh thu");

        XYChart.Series<String, Number> electronicsSeries = new XYChart.Series<>();
        electronicsSeries.setName("Electronics");

        XYChart.Series<String, Number> artSeries = new XYChart.Series<>();
        artSeries.setName("Art");

        XYChart.Series<String, Number> vehicleSeries = new XYChart.Series<>();
        vehicleSeries.setName("Vehicle");

        // 3. Thêm dữ liệu mẫu (Sau này bạn sẽ thay bằng dữ liệu từ database)
        // Dữ liệu cho Tổng doanh thu
        totalSeries.getData().add(new XYChart.Data<>("Jan", 5000));
        totalSeries.getData().add(new XYChart.Data<>("Feb", 7200));
        totalSeries.getData().add(new XYChart.Data<>("Mar", 6500));

        // Dữ liệu cho Electronics
        electronicsSeries.getData().add(new XYChart.Data<>("Jan", 2000));
        electronicsSeries.getData().add(new XYChart.Data<>("Feb", 3500));
        electronicsSeries.getData().add(new XYChart.Data<>("Mar", 2800));

        // Dữ liệu cho Art
        artSeries.getData().add(new XYChart.Data<>("Jan", 1500));
        artSeries.getData().add(new XYChart.Data<>("Feb", 2200));
        artSeries.getData().add(new XYChart.Data<>("Mar", 1700));

        // Dữ liệu cho Vehicle
        vehicleSeries.getData().add(new XYChart.Data<>("Jan", 1500));
        vehicleSeries.getData().add(new XYChart.Data<>("Feb", 1500));
        vehicleSeries.getData().add(new XYChart.Data<>("Mar", 2000));

        double totalRevenue = 0;
        //Lấy tổng doanh thu
        for (XYChart.Data<String, Number> data : totalSeries.getData()) {
            totalRevenue += data.getYValue().doubleValue();
        }

        EquilibriumAnimation.updateCurrency(totalRevenueLabel, totalRevenue);

        // 4. Đưa tất cả series vào biểu đồ
        revenueChart.getData().addAll(totalSeries, electronicsSeries, artSeries, vehicleSeries);
    }

    /**
     * TÍNH NĂNG: Load lịch sử giao dịch
     */
    private void loadOrderHistory() {
        // TODO: LOGIC DATABASE
        // 1. Tạo class Model 'Order' với các field: id, product, buyer, price, date, status
        // 2. Gọi Repository lấy danh sách đơn hàng của Seller hiện tại
        // 3. Đổ dữ liệu vào bảng: orderTable.setItems(orderList);

        System.out.println("Đang tải dữ liệu lịch sử đơn hàng...");
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
        registerProductView.setVisible(false);
        registerProductView.setManaged(false);
        orderHistoryView.setVisible(false);
        orderHistoryView.setManaged(false);
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
        btnOrderHistory.setStyle(inactiveStyle);
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

    // Bấm vào nút Chuông -> Ẩn hoặc hiện khung thông báo (Chỉ xử lý UI mượt mà)
    @FXML
    private void handleToggleNotification(ActionEvent event) {
        if (profileDropdown != null && profileDropdown.isVisible()) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }

        boolean isCurrentlyVisible = notificationDropdown.isVisible();
        // Đảo ngược trạng thái hiển thị
        notificationDropdown.setVisible(!isCurrentlyVisible);
        notificationDropdown.setManaged(!isCurrentlyVisible);
    }

    // Hàm bật/tắt menu tài khoản
    @FXML
    private void handleToggleProfile(ActionEvent event) {
        // Nếu đang mở thông báo thì đóng thông báo lại để tránh đè giao diện
        if (notificationDropdown.isVisible()) {
            notificationDropdown.setVisible(false);
            notificationDropdown.setManaged(false);
        }

        // Đảo ngược trạng thái hiển thị của profile
        boolean isVisible = profileDropdown.isVisible();
        profileDropdown.setVisible(!isVisible);
        profileDropdown.setManaged(!isVisible);
    }

    // Hàm xử lý khi nhấn "Thông tin cá nhân"
    @FXML
    private void handleViewProfile(ActionEvent event) {
        System.out.println("Chuyển hướng đến trang Thông tin cá nhân...");
        // Thêm logic chuyển Tab hoặc mở Window mới tại đây
        profileDropdown.setVisible(false); // Ẩn menu đi sau khi chọn
        profileDropdown.setManaged(false);
    }

    // Hàm xử lý khi nhấn "Đăng xuất"
    @FXML
    private void handleLogout(ActionEvent event) {
        FXMLLoader loader = EquilibriumAnimation.changeScene(event, "login-view.fxml", "Đăng xuất");

        if (loader != null) {
            LoginController controller = loader.getController();
        }

        System.out.println("Đang thực hiện đăng xuất...");
    }
}
