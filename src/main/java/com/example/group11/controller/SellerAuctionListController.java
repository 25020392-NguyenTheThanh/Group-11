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

    private boolean needsRefresh = true;

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
        executeTabLogic("btnMyListings");

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

        executeTabLogic(buttonId);
    }

    // Tách riêng logic load dữ liệu để handleSwitchTab gọn hơn
    private void executeTabLogic(String buttonId) {
        switch (buttonId) {
            case "btnMyListings":
                if (contentGrid.getChildren().isEmpty() || needsRefresh) {
                    loadMyListingView();
                    needsRefresh = false;
                }
                break;
            case "btnAnalytics":
                loadAnalyticsData();
                break;
            case "btnOrderHistory":
                System.out.println("Tab Shipping đang phát triển");
                break;
        }
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

    private void loadMyListingView() {
        // 1. Xóa sạch các card cũ trong lưới để tránh trùng lặp [cite: 44, 47]
        contentGrid.getChildren().clear();

        // 2. Giả lập danh sách dữ liệu (Sau này bạn sẽ thay bằng List<AuctionItem> từ database)
        // Dữ liệu mẫu dựa trên ảnh "Laptop Gaming ASUS" bạn cung cấp
        for (int i = 0; i < 6; i++) {
            VBox productCard = createProductCard(
                    "AU-2026-8899", // auctionId [cite: 209]
                    "LAPTOP GAMING ASUS ROG ZEPHYRUS", // productName [cite: 221]
                    "Chip M3 Max, 32GB RAM, SSD 1TB, màn hình 120Hz...", // description [cite: 222]
                    "30.000.000", // startingPrice [cite: 230]
                    "55.000.000", // currentPrice [cite: 237]
                    "10:00 - 10/05/2026", // startTime [cite: 245]
                    "14:00 - 15/05/2026", // endTime [cite: 252]
                    "RUNNING", // status [cite: 212]
                    "00:15:30", // timer [cite: 215]
                    "https://your-image-url.com/laptop.png" // imageUrl
            );


            // 3. Tính toán vị trí cột và hàng cho GridPane (3 cột)
            int column = i % 3;
            int row = i / 3;

            contentGrid.add(productCard, column, row);
        }

        // 4. Cập nhật nhãn tổng số lượng sản phẩm trên giao diện [cite: 43]
        CalculatorView.updateCount(totalProductsLabel, 6);
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
    private void handleSelectImage(Event event) {
        if (selectedImageFile == null) {
            // Trường hợp chưa có ảnh: Mở trình chọn file
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn ảnh sản phẩm");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

            File file = fileChooser.showOpenDialog(imageDropzone.getScene().getWindow());
            if (file != null) {
                selectedImageFile = file;
                displayImage(file);
            }
        } else {
            // Trường hợp đã có ảnh: Hỏi xác nhận xóa để chọn lại
            confirmRemoveImage();
        }
    }

    @FXML
    private void handleSubmitProduct() {
        // 1. Gọi các hàm kiểm tra dữ liệu
        if (!validateEmptyFields()) return;
        if (!validateNumericFormats()) return;
        if (!validateDateTime()) return;

        // 2. Nếu hợp lệ thì xử lý đăng ký
        System.out.println("Đang xử lý đăng ký sản phẩm: " + productNameField.getText().trim());

        // TODO: Logic Database của bạn ở đây...
        showNotification("Thành công", "Đăng ký sản phẩm thành công!");
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
        FXMLLoader loader = GenerationSupport.changeScene(event, "login-view.fxml", "Đăng xuất");

        if (loader != null) {
            LoginController controller = loader.getController();
        }

        System.out.println("Đang thực hiện đăng xuất...");
    }

    private void confirmRemoveImage() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có muốn xóa ảnh hiện tại để chọn lại không?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                selectedImageFile = null;
                productImageView.setImage(null);
                productImageView.setVisible(false);
                uploadPrompt.setVisible(true);
            }
        });
    }


    // Hiển thị ảnh lên giao diện và ẩn thông báo hướng dẫn.
    private void displayImage(File file) {
        Image image = new Image(file.toURI().toString());
        productImageView.setImage(image);

        // Hiện ImageView và ẩn VBox hướng dẫn
        productImageView.setVisible(true);
        uploadPrompt.setVisible(false);

        System.out.println("Đã chọn ảnh: " + file.getAbsolutePath());
    }

    // Kiểm tra các ô nhập liệu có bị bỏ trống hay không
    private boolean validateEmptyFields() {
        // Kiểm tra tên
        if (productNameField.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Tên sản phẩm không được để trống!");
            productNameField.requestFocus();
            return false;
        }

        // Kiểm tra danh mục
        String category = categoryMenuButton.getText();
        if (category == null || category.isEmpty() || category.equals("Chọn danh mục")) {
            showAlert("Lỗi nhập liệu", "Vui lòng chọn danh mục cho sản phẩm!");
            categoryMenuButton.requestFocus();
            return false;
        }

        // Kiểm tra chuỗi nhập giá khởi điểm
        if (startingPriceField.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Giá khởi điểm không được để trống!");
            startingPriceField.requestFocus();
            return false;
        }

        // Kiểm tra chuỗi nhập bước giá
        if (minimumBidIncrementField.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Bước giá tối thiểu không được để trống!");
            minimumBidIncrementField.requestFocus();
            return false;
        }

        // Kiểm tra ngày bắt đầu/kết thúc
        if (startDatePicker.getValue() == null) {
            showAlert("Lỗi nhập liệu", "Vui lòng chọn ngày bắt đầu đấu giá!");
            startDatePicker.requestFocus();
            return false;
        }
        if (endDatePicker.getValue() == null) {
            showAlert("Lỗi nhập liệu", "Vui lòng chọn ngày kết thúc đấu giá!");
            endDatePicker.requestFocus();
            return false;
        }

        // Kiểm tra mô tả
        if (descriptionArea.getText().trim().isEmpty()) {
            showAlert("Lỗi nhập liệu", "Mô tả sản phẩm không được để trống!");
            descriptionArea.requestFocus();
            return false;
        }

        // Kiểm tra ảnh
        if (selectedImageFile == null) {
            showAlert("Lỗi thiếu thông tin", "Vui lòng chọn hình ảnh đại diện cho sản phẩm!");
            imageDropzone.requestFocus();
            return false;
        }

        return true; // Tất cả đều đã điền
    }


    //Kiểm tra định dạng số và logic toán học của Giá
    private boolean validateNumericFormats() {
        double startingPrice;
        double bidIncrement;

        // 1. Ép kiểu giá khởi điểm
        try {
            startingPrice = Double.parseDouble(startingPriceField.getText().trim());
            if (startingPrice <= 0) {
                showAlert("Lỗi định dạng", "Giá khởi điểm phải là một số lớn hơn 0!");
                startingPriceField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi định dạng", "Giá khởi điểm phải là một số hợp lệ (Ví dụ: 100000)!");
            startingPriceField.requestFocus();
            return false;
        }

        // 2. Ép kiểu bước giá tối thiểu
        try {
            bidIncrement = Double.parseDouble(minimumBidIncrementField.getText().trim());
            if (bidIncrement <= 0) {
                showAlert("Lỗi định dạng", "Bước giá tối thiểu phải lớn hơn 0!");
                minimumBidIncrementField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi định dạng", "Bước giá tối thiểu phải là một số hợp lệ!");
            minimumBidIncrementField.requestFocus();
            return false;
        }

        // 3. Logic so sánh giữa 2 số
        if (bidIncrement > startingPrice) {
            showAlert("Lỗi logic", "Bước giá tối thiểu không được lớn hơn giá khởi điểm!");
            minimumBidIncrementField.requestFocus();
            return false;
        }

        return true; // Định dạng số hoàn toàn hợp lệ
    }

    //Kiểm tra logic thời gian đấu giá
    private boolean validateDateTime() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        // Ngày bắt đầu không được ở quá khứ
        if (startDate.isBefore(LocalDate.now())) {
            showAlert("Lỗi thời gian", "Ngày bắt đầu không được ở trong quá khứ!");
            startDatePicker.requestFocus();
            return false;
        }

        // Ngày kết thúc phải sau ngày bắt đầu
        if (!endDate.isAfter(startDate)) {
            showAlert("Lỗi thời gian", "Ngày kết thúc phải sau ngày bắt đầu ít nhất 1 ngày!");
            endDatePicker.requestFocus();
            return false;
        }

        return true; // Thời gian hoàn toàn hợp lệ
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

    //Hàm hiển thị thông báo thành công (Information)
    private void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // Ẩn phần header mặc định để hộp thoại gọn gàng hơn
        alert.setContentText(content);
        alert.showAndWait(); // Hiển thị cửa sổ và dừng luồng cho đến khi người dùng bấm OK
    }

    // Hàm hiển thị thông báo Alert nhanh (Warning)
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


    private VBox createProductCard(String id, String name, String desc, String startPrice, String currentPrice, String startTime, String endTime, String status, String timeLeft, String imageUrl) {
        // Container chính (mainCardContainer trong FXML)
        VBox card = new VBox();
        card.setMaxWidth(350.0);
        card.setStyle("-fx-background-color: #1A1A1A; -fx-border-color: #262626; -fx-border-width: 1; -fx-background-radius: 15; -fx-border-radius: 15;");

        // 1. Header: ID, Status và Timer
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setPrefHeight(35.0);
        header.setSpacing(10.0);
        header.setPadding(new Insets(0, 15, 0, 15));
        header.setStyle("-fx-background-color: #0A0A0A; -fx-border-color: transparent transparent #262626 transparent; -fx-background-radius: 15 15 0 0; -fx-border-radius: 15 15 0 0;");

        Label lblId = new Label("ID: " + id);
        lblId.setStyle("-fx-text-fill: #d0c6ab; -fx-font-weight: bold;");
        lblId.setFont(new Font(9.0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox statusBox = new HBox(5);
        statusBox.setAlignment(javafx.geometry.Pos.CENTER);
        Circle dot = new Circle(3.0, javafx.scene.paint.Color.web("#00e475"));
        Label lblStatus = new Label(status);
        lblStatus.setStyle("-fx-text-fill: #00e475; -fx-font-weight: bold;");
        lblStatus.setFont(new Font(10.0));
        statusBox.getChildren().addAll(dot, lblStatus);

        Label lblTimer = new Label(timeLeft);
        lblTimer.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        lblTimer.setFont(new Font(12.0));

        header.getChildren().addAll(lblId, spacer, statusBox, lblTimer);

        // 2. Hình ảnh sản phẩm
        StackPane imgStack = new StackPane();
        imgStack.setPrefHeight(180.0);
        imgStack.setStyle("-fx-background-color: #000000;");

        ImageView imgView = new ImageView();
        imgView.setFitHeight(180.0);
        imgView.setFitWidth(350.0);
        imgView.setPreserveRatio(true);
        // Thêm logic load ảnh từ URL nếu có, nếu không dùng ảnh mặc định
        try {
            imgView.setImage(new Image(imageUrl, true));
        } catch (Exception e) {
            imgView.setImage(new Image("https://lh3.googleusercontent.com/aida-public/AB6AXuBVMAGKkFbo45OTsBsSobXfbNDExErUNOrmwi5tBP3M_Xkz5Pp87L2CMVZm7fuR54EIXTbqY1PfIOd7C-1qaYKZ91Ycjjb2VeoXOM5ZMcwEWh9sRD1NBZMmwemftfNIADQcw5yHuueYdwrYntl4qm5r06zY4x9gCBJASSvhyOqt1L1yzlxfez9H_HhbLRRC2vpCAFBuAW3AMp0ZjZu-NDi1eteCstkcdYUG5Ysm7gsRCk3JzbdraApIRPHxNIWevRgwQ29qkB7xW4Y"));
        }
        imgStack.getChildren().add(imgView);

        // 3. Nội dung thông tin (VBox chính bên dưới ảnh)
        VBox content = new VBox(12.0);
        content.setPadding(new Insets(15, 15, 15, 15));

        // Tên và mô tả
        VBox nameDesc = new VBox(3.0);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        lblName.setFont(new Font(16.0));
        lblName.setWrapText(true);

        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-text-fill: #d0c6ab;");
        lblDesc.setFont(new Font(12.0));
        lblDesc.setWrapText(true);
        nameDesc.getChildren().addAll(lblName, lblDesc);

        // Khu vực giá
        VBox priceContainer = new VBox(4.0);
        priceContainer.setStyle("-fx-background-color: #0A0A0A; -fx-padding: 8 12; -fx-border-color: transparent transparent transparent #ffd700; -fx-border-width: 0 0 0 3;");

        VBox startPriceBox = new VBox();
        Label tStart = new Label("GIÁ KHỞI ĐIỂM");
        tStart.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-weight: bold;");
        tStart.setFont(new Font(8.0));
        Label vStart = new Label(startPrice + " đ");
        vStart.setStyle("-fx-text-fill: rgba(255,255,255,0.8);");
        startPriceBox.getChildren().addAll(tStart, vStart);

        VBox currentPriceBox = new VBox();
        Label tCurrent = new Label("GIÁ HIỆN TẠI CAO NHẤT");
        tCurrent.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold;");
        tCurrent.setFont(new Font(9.0));
        Label vCurrent = new Label(currentPrice + " đ");
        vCurrent.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold;");
        vCurrent.setFont(new Font(20.0));
        currentPriceBox.getChildren().addAll(tCurrent, vCurrent);

        priceContainer.getChildren().addAll(startPriceBox, currentPriceBox);

        // Khu vực thời gian (HBox)
        HBox timeBox = new HBox();
        timeBox.setStyle("-fx-border-color: #262626 transparent #262626 transparent; -fx-border-width: 1 0 1 0; -fx-padding: 8 0;");

        VBox startTimeV = new VBox(2.0);
        HBox.setHgrow(startTimeV, javafx.scene.layout.Priority.ALWAYS);
        Label tS = new Label("BẮT ĐẦU");
        tS.setStyle("-fx-text-fill: #d0c6ab;");
        tS.setFont(new Font(8.0));
        Label vS = new Label(startTime);
        vS.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        vS.setFont(new Font(11.0));
        startTimeV.getChildren().addAll(tS, vS);

        VBox endTimeV = new VBox(2.0);
        HBox.setHgrow(endTimeV, javafx.scene.layout.Priority.ALWAYS);
        endTimeV.setStyle("-fx-border-color: transparent transparent transparent #262626; -fx-border-width: 0 0 0 1; -fx-padding: 0 0 0 15;");
        Label tE = new Label("KẾT THÚC");
        tE.setStyle("-fx-text-fill: #d0c6ab;");
        tE.setFont(new Font(8.0));
        Label vE = new Label(endTime);
        vE.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        vE.setFont(new Font(11.0));
        endTimeV.getChildren().addAll(tE, vE);

        timeBox.getChildren().addAll(startTimeV, endTimeV);

        // Nút bấm hành động
        HBox actions = new HBox(10.0);
        Button btnDetails = new Button("CHI TIẾT");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDetails, javafx.scene.layout.Priority.ALWAYS);
        btnDetails.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #555555; -fx-border-radius: 5; -fx-padding: 10; -fx-cursor: hand;");

        Button btnDelete = new Button("XÓA");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDelete, javafx.scene.layout.Priority.ALWAYS);
        btnDelete.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 5; -fx-cursor: hand;");

        // Gán sự kiện xóa từ controller
        btnDelete.setOnAction(e -> handleDeleteAuctionAction(id, card));

        actions.getChildren().addAll(btnDetails, btnDelete);

        // Lắp ráp toàn bộ card
        content.getChildren().addAll(nameDesc, priceContainer, timeBox, actions);
        card.getChildren().addAll(header, imgStack, content);

        return card;
    }

    private void handleDeleteAuctionAction(String auctionId, VBox cardNode) {
        // 1. Hiển thị Alert xác nhận theo phong cách ItemSellerController
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Bạn có chắc chắn muốn xóa đấu giá này không?");
        alert.setContentText("Hành động này không thể hoàn tác ID: " + auctionId);

        ButtonType buttonYes = new ButtonType("Đồng ý");
        ButtonType buttonNo = new ButtonType("Hủy bỏ");
        alert.getButtonTypes().setAll(buttonYes, buttonNo);

        alert.showAndWait().ifPresent(response -> {
            if (response == buttonYes) {
                // 2. Xóa khỏi giao diện contentGrid
                contentGrid.getChildren().remove(cardNode);

                // 3. Cập nhật lại label tổng số lượng
                int currentCount = Integer.parseInt(totalProductsLabel.getText());
                CalculatorView.updateCount(totalProductsLabel, currentCount - 1);

                System.out.println("Đã xóa đấu giá: " + auctionId);
            }
        });
    }
}
