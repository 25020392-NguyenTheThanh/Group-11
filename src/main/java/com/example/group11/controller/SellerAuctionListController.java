package com.example.group11.controller;

import com.auction.model.user.User;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static com.example.group11.controller.ImagesController.confirmRemoveImage;
import static com.example.group11.controller.ImagesController.displayImage;
import static com.example.group11.controller.SellerUIHelper.*;

public class SellerAuctionListController implements Initializable {
    @FXML
    private Button SystemNotification;

    @FXML
    private Button addFundsBtn;

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
    private Button btnOrderHistory;

    @FXML
    private Button btnProfileInfo;

    @FXML
    private Button btnSettings;

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
    private StackPane imageDropzone;

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
    private ImageView productImageView;

    @FXML
    private TextField productNameField;

    @FXML
    private VBox profileDropdown;

    @FXML
    private VBox registerProductView;

    @FXML
    private LineChart<String, Number> revenueChart;

    @FXML
    private Label soldProductsLabel;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField startingPriceField;

    @FXML
    private Button submitButton;

    @FXML
    private Label totalBidsLabel;

    @FXML
    private Label totalProductsLabel;

    @FXML
    private Label totalRevenueLabel;

    @FXML
    private VBox uploadPrompt;

    @FXML
    private Label walletBalance;

    private VBox lastView;

    private Button lastButton;

    private VBox currentView; // Lưu view hiện tại để gán cho lastView khi chuyển tiếp

    // Biến lưu trữ file ảnh đã chọn để dùng khi nhấn "XÁC NHẬN ĐĂNG KÝ"
    private File selectedImageFile;

    private List<Button> allButtons;

    private List<VBox> allViews;

    private Map<String, VBox> viewMapping;

    private User user;

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentView = myListingsView;
        lastView = myListingsView;
        lastButton = btnMyListings;
        // Khởi tạo các tập hợp dữ liệu (List/Map) để Helper làm việc
        allButtons = List.of(btnMyListings, btnAnalytics, btnOrderHistory, btnSettings);
        allViews = List.of(myListingsView, analyticsPane, orderHistoryView, registerProductView);
        viewMapping = Map.of(
                "btnMyListings", myListingsView,
                "btnAnalytics", analyticsPane,
                "btnOrderHistory", orderHistoryView,
                "btnSettings", new VBox() // Hoặc view tương ứng
        );

        // THIẾT LẬP GIAO DIỆN MẶC ĐỊNH (My Listings)
        currentView = myListingsView;

        // Sử dụng Helper để hiển thị và đổi màu nút
        SellerUIHelper.showView(currentView, allViews);
        SellerUIHelper.setActiveStyle(btnMyListings);

        // NẠP DỮ LIỆU BAN ĐẦU
        // Gọi executeTabLogic với ID của tab mặc định
        SellerUIHelper.executeTabLogic("btnMyListings", contentGrid, this);

        GenerationSupport.setupMenuButtonUpdate(filterSort);
        GenerationSupport.setupMenuButtonUpdate(filterStatus);
        GenerationSupport.setupMenuButtonUpdate(categoryMenuButton);

    }

    @FXML
    private void handleSwitchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        // Dùng ID của button để phân biệt (chính xác hơn dùng Text)
        String buttonId = clickedButton.getId();
        VBox targetView = SellerUIHelper.getVBoxFromId(buttonId, viewMapping);

        // Chỉ lưu nếu view hiện tại khác với view sắp tới (tránh lưu đè chính nó)
        if (currentView != targetView) {
            lastView = currentView;
            lastButton = SellerUIHelper.findActiveButton(allButtons); // Hàm phụ để tìm nút đang sáng
        }

        // 1. Cập nhật UI Sidebar
        SellerUIHelper.resetAllButtons(allButtons);
        SellerUIHelper.setActiveStyle(clickedButton);

        // Cập nhật currentView và hiển thị
        currentView = targetView;
        SellerUIHelper.showView(currentView, allViews);

        SellerUIHelper.executeTabLogic(buttonId, contentGrid, this);
    }

    @FXML
    void handleAddItem(ActionEvent event) {
        // Lưu lại vết trước khi sang trang đăng ký
        lastView = currentView;
        lastButton = SellerUIHelper.findActiveButton(allButtons);

        // Thay vì chuyển Scene, ta chuyển View trong cùng 1 cửa sổ
        SellerUIHelper.resetAllButtons(allButtons); // Bỏ active ở các nút sidebar
        currentView = registerProductView;
        SellerUIHelper.showView(registerProductView, allViews);
    }

    @FXML
    private void handleBackToListings(ActionEvent event) {
        if (lastView != null) {
            // 1. Cập nhật UI Sidebar
            SellerUIHelper.resetAllButtons(allButtons);
            if (lastButton != null) {
                setActiveStyle(lastButton);
            }
            // 2. Quay lại view cũ
            SellerUIHelper.showView(lastView, allViews);
            // 3. Cập nhật lại currentView
            currentView = lastView;
        }
    }

    public void loadMyListingView() {
        // 1. Xóa sạch các card cũ trong lưới để tránh trùng lặp
        contentGrid.getChildren().clear();

        // 2. Giả lập danh sách dữ liệu (Sau này bạn sẽ thay bằng List<AuctionItem> từ database)
        // Dữ liệu mẫu dựa trên ảnh "Laptop Gaming ASUS" bạn cung cấp
        for (int i = 0; i < 6; i++) {
            VBox productCard = ProductCardFactory.createProductCard(
                    "AU-2026-8899", // auctionId [cite: 209]
                    "LAPTOP GAMING ASUS ROG ZEPHYRUS", // productName
                    "Chip M3 Max, 32GB RAM, SSD 1TB, màn hình 120Hz...", // description
                    "30.000.000", // startingPrice
                    "55.000.000", // currentPrice
                    "10:00 - 10/05/2026", // startTime
                    "14:00 - 15/05/2026", // endTime
                    "RUNNING", // status
                    "00:15:30", // timer
                    "https://your-image-url.com/laptop.png", // imageUrl
                    (cardNode) -> {
                        // HÀM CALLBACK: Đoạn code này chỉ chạy khi người dùng bấm "Đồng ý" ở Alert bên kia
                        this.contentGrid.getChildren().remove(cardNode);

                        // Cập nhật lại nhãn đếm tổng số lượng sản phẩm
                        int currentCount = Integer.parseInt(totalProductsLabel.getText());
                        CalculatorView.updateCount(totalProductsLabel, currentCount - 1);

                        System.out.println("Controller đã xóa card thành công!");
                    }
            );

            // 3. Tính toán vị trí cột và hàng cho GridPane (3 cột)
            int column = i % 3;
            int row = i / 3;

            contentGrid.add(productCard, column, row);
        }

        // 4. Cập nhật nhãn tổng số lượng sản phẩm trên giao diện
        CalculatorView.updateCount(totalProductsLabel, 6);
    }

    // TÍNH NĂNG: Load dữ liệu biểu đồ
    public void loadAnalyticsData() {
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

        CalculatorView.updateCurrency(totalRevenueLabel, totalRevenue);

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

    @FXML
    private void handleSubmitProduct(ActionEvent event) {
        // 1. Gọi các hàm kiểm tra dữ liệu
        if (!ProductRegistrationValidator.validateAll(productNameField, categoryMenuButton, startingPriceField,
                minimumBidIncrementField, startDatePicker, endDatePicker,
                descriptionArea, selectedImageFile, imageDropzone)) return;

        // 2. Nếu hợp lệ thì xử lý đăng ký
        System.out.println("Đang xử lý đăng ký sản phẩm: " + productNameField.getText().trim());

        // TODO: Logic Database của bạn ở đây...
        NotificationController.showNotification("Thành công", "Đăng ký sản phẩm thành công!");

        // Đánh dấu cần làm mới danh sách cho lần chuyển tab sau
        SellerUIHelper.setNeedsRefresh(true);

        // 1. XÓA SẠCH DỮ LIỆU TRÊN FORM ĐĂNG KÝ VỪA NHẬP
        clearRegistrationForm();

        // 2. Quay lại giao diện danh sách sản phẩm
        handleBackToListings(null);
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
        boolean confirm = NotificationController.showConfirmation(
                "Xác nhận đăng xuất",
                "Bạn có chắc chắn muốn đăng xuất không?",
                "Hệ thống sẽ kết thúc phiên làm việc hiện tại của bạn.",
                "Có, Đăng xuất",
                "Không, Ở lại"
        );
        if (confirm) {
            System.out.println("Đang thực hiện đăng xuất...");
            FXMLLoader loader = GenerationSupport.changeScene(event, "login-view.fxml", "Đăng xuất");

            if (loader != null) {
                LoginController controller = loader.getController();
            } else {
                System.out.println("Đã hủy yêu cầu đăng xuất.");
            }
        }
    }

    /**
     * Xóa sạch toàn bộ dữ liệu đã nhập trên form đăng ký sản phẩm
     */
    private void clearRegistrationForm() {
        // 1. Xóa văn bản trong các ô TextField và TextArea
        productNameField.clear();
        startingPriceField.clear();
        minimumBidIncrementField.clear();
        descriptionArea.clear();

        // 2. Đặt lại chữ mặc định cho MenuButton danh mục
        categoryMenuButton.setText("Chọn danh mục"); // Thay bằng chữ mặc định ban đầu của bạn nếu khác

        // 3. Xóa ngày đã chọn trong các ô DatePicker
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);

        // 4. Khôi phục lại trạng thái khung chọn ảnh ban đầu
        selectedImageFile = null;
        productImageView.setImage(null);
        productImageView.setVisible(false); // Ẩn khung hiển thị ảnh đi
        uploadPrompt.setVisible(true);      // Hiện lại dòng chữ hướng dẫn "Bấm để chọn ảnh"
    }

    @FXML
    private void handleSelectImage(Event event) {
        if (selectedImageFile == null) {
            // Trường hợp chưa có ảnh: Mở trình chọn file
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn ảnh sản phẩm");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

            File file = fileChooser.showOpenDialog(imageDropzone.getScene().getWindow());
            if (file != null) {
                selectedImageFile = file;
                ImagesController.displayImage(file, productImageView, uploadPrompt);
            }
        } else {
            // Trường hợp đã có ảnh: Hỏi xác nhận xóa để chọn lại
            selectedImageFile = ImagesController.confirmRemoveImage(selectedImageFile, productImageView, uploadPrompt);
        }
    }
}
