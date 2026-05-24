package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.data.ItemRepository;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.model.user.User;
import com.auction.network.*;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.ItemStatus;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

import static com.example.group11.controller.ImagesController.confirmRemoveImage;
import static com.example.group11.controller.ImagesController.displayImage;
import static com.example.group11.controller.SellerUIHelper.*;

/**
 * Bộ điều khiển (Controller) cho màn hình danh sách sản phẩm và đấu giá của Người bán (Seller).
 * Quản lý các chức năng: hiển thị danh sách sản phẩm cá nhân, bộ lọc tìm kiếm,
 * biểu đồ phân tích doanh thu, lịch sử đơn hàng, đăng ký và sửa đổi sản phẩm đấu giá.
 */
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
    private TableColumn<Order, String> colBuyer;

    @FXML
    private TableColumn<Order, LocalDateTime> colDate;

    @FXML
    private TableColumn<Order, Integer> colOrderId;

    @FXML
    private TableColumn<Order, Double> colPrice;

    @FXML
    private TableColumn<Order, String> colProduct;

    @FXML
    private TableColumn<Order, String> colStatus;

    @FXML
    private GridPane contentGrid;

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
    private VBox notificationListContainer;

    @FXML
    private VBox orderHistoryView;

    @FXML
    private TableView<Order> orderTable;

    @FXML
    private TextField searchOrderField;

    @FXML
    private ImageView productImageView;

    @FXML
    private TextField productNameField;

    @FXML
    private VBox profileDropdown;

    @FXML
    private VBox registerProductView;

    @FXML
    private BarChart<String, Number> revenueChart;

    @FXML
    private Label soldProductsLabel;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField startingPriceField;

    @FXML
    private Button submitButton;

    @FXML
    private Label registerTitleLabel;

    @FXML
    private Label registerSubtitleLabel;

    private Item editingItem = null;

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

    @FXML
    private VBox dynamicAttributesContainer;

    private VBox lastView;

    private Button lastButton;

    private VBox currentView; // Lưu view hiện tại để gán cho lastView khi chuyển tiếp

    // Biến lưu trữ file ảnh đã chọn để dùng khi nhấn "XÁC NHẬN ĐĂNG KÝ"
    private File selectedImageFile;

    private List<Button> allButtons;

    private List<VBox> allViews;

    private Map<String, VBox> viewMapping;

    private User user;

    private String linkImageUrl;


    // DANH SÁCH LƯU TRỮ SẢN PHẨM TẠM THỜI TRONG BỘ NHỚ
    private List<Item> auctionItems = new ArrayList<>();
    private Map<Integer, Auction> itemAuctionMap = new HashMap<>();

    /**
     * Thiết lập thông tin người dùng hiện tại (Người bán).
     *
     * @param user Đối tượng người dùng
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Phương thức khởi tạo mặc định của JavaFX Controller.
     * Thiết lập cấu hình ban đầu cho giao diện người bán bao gồm điều hướng các Tab,
     * các bộ lọc tìm kiếm, bảng lịch sử đơn hàng, thực đơn danh mục và đăng ký bộ lắng nghe
     * sự kiện thông báo thời gian thực từ máy chủ.
     *
     * @param location  Đường dẫn URL của file FXML nguồn
     * @param resources Tài nguyên bản địa hóa
     */
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

        setupFilters();
        setupOrderTableColumns();
        GenerationSupport.setupMenuButtonUpdate(categoryMenuButton);

        setupCategoryMenuItems();

        setupRealtimeNotifications();
    }

    /**
     * Thiết lập kết nối nhận thông báo thời gian thực từ Server.
     * Xử lý tự động tải lại danh sách sản phẩm khi nhận thấy có phiên đấu giá kết thúc
     * hoặc trạng thái sản phẩm thay đổi.
     */
    private void setupRealtimeNotifications() {
        ServerConnection.getInstance().addNotificationHandler(notification -> {
            addNotificationToUI(notification);

            String type = notification.getType();
            if ("AUCTION_ENDED".equals(type) || "ITEM_STATUS_CHANGED".equals(type)) {
                System.out.println("[REALTIME] Nhận thông báo thay đổi trạng thái, đang tự động tải lại danh sách sản phẩm...");
                loadMyListingView();
            }
        });
    }


    /**
     * Thêm một thông báo mới vào giao diện danh sách thông báo thả xuống (Dropdown).
     * Hỗ trợ định dạng tiêu đề và mô tả chi tiết tương ứng với từng loại thông báo.
     *
     * @param notification Đối tượng chứa dữ liệu thông báo từ Server
     */
    private void addNotificationToUI(com.auction.network.Notification notification) {
        if (notificationListContainer == null) return;

        VBox notifBox = new VBox(4.0);
        notifBox.setStyle("-fx-background-color: #1E2D45; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titleLabel = new Label();
        titleLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        titleLabel.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 12.0));

        String type = notification.getType();
        if ("AUCTION_ENDED".equals(type)) {
            titleLabel.setText("🏁 Phiên đấu giá kết thúc");
        } else if ("ITEM_STATUS_CHANGED".equals(type)) {
            titleLabel.setText("🔄 Trạng thái sản phẩm thay đổi");
        } else {
            titleLabel.setText("🔔 Thông báo hệ thống");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label("Vừa xong");
        timeLabel.setTextFill(javafx.scene.paint.Color.web("#94a3b8"));
        timeLabel.setFont(new Font(10.0));

        header.getChildren().addAll(titleLabel, spacer, timeLabel);

        Label descLabel = new Label();
        if (notification.getData() != null) {
            if ("ITEM_STATUS_CHANGED".equals(type)) {
                descLabel.setText("Sản phẩm ID: " + notification.getData() + " đã cập nhật trạng thái mới.");
            } else {
                descLabel.setText(notification.getData().toString());
            }
        }
        descLabel.setTextFill(javafx.scene.paint.Color.web("#94A3B8"));
        descLabel.setFont(new Font(11.0));
        descLabel.setWrapText(true);

        notifBox.getChildren().addAll(header, descLabel);

        // Đưa thông báo mới lên đầu danh sách
        notificationListContainer.getChildren().add(0, notifBox);
    }

    /**
     * Thiết lập các sự kiện lắng nghe bộ lọc tìm kiếm và sắp xếp.
     * Tự động áp dụng bộ lọc mỗi khi người dùng thay đổi ô tìm kiếm hoặc chọn danh mục sắp xếp/trạng thái.
     */
    private void setupFilters() {
        filterSearchId.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFiltersAndSort();
        });

        for (MenuItem item : filterStatus.getItems()) {
            item.setOnAction(e -> {
                filterStatus.setText(item.getText().toUpperCase());
                applyFiltersAndSort();
            });
        }

        for (MenuItem item : filterSort.getItems()) {
            item.setOnAction(e -> {
                filterSort.setText(item.getText().toUpperCase());
                applyFiltersAndSort();
            });
        }
    }

    /**
     * Thực hiện lọc và sắp xếp danh sách sản phẩm dựa trên từ khóa tìm kiếm,
     * trạng thái sản phẩm/phiên đấu giá và kiểu sắp xếp đã lựa chọn.
     * Sau đó cập nhật lại giao diện lưới hiển thị sản phẩm.
     */
    private void applyFiltersAndSort() {
        if (auctionItems == null) return;

        String query = filterSearchId.getText() != null ? filterSearchId.getText().trim().toLowerCase() : "";
        String selectedStatus = filterStatus.getText() != null ? filterStatus.getText().trim().toUpperCase() : "TRẠNG THÁI";

        List<Item> filteredItems = new ArrayList<>();
        for (Item item : auctionItems) {
            boolean matchesSearch = query.isEmpty() ||
                    String.valueOf(item.getId()).contains(query) ||
                    (item.getName() != null && item.getName().toLowerCase().contains(query)) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(query));

            if (!matchesSearch) continue;

            boolean matchesStatus = true;
            if (!selectedStatus.equals("TẤT CẢ") && !selectedStatus.equals("TRẠNG THÁI")) {
                if (selectedStatus.equals("AVAILABLE")) {
                    matchesStatus = (item.getStatus() == ItemStatus.AVAILABLE);
                } else {
                    Auction auction = itemAuctionMap.get(item.getId());
                    matchesStatus = (auction != null && auction.getStatus() != null &&
                            auction.getStatus().name().equalsIgnoreCase(selectedStatus));
                }
            }

            if (matchesStatus) {
                filteredItems.add(item);
            }
        }

        String selectedSort = filterSort.getText() != null ? filterSort.getText().trim().toUpperCase() : "SẮP XẾP";
        if (selectedSort.equals("GIÁ: THẤP ĐẾN CAO")) {
            filteredItems.sort(Comparator.comparingDouble(Item::getStartingPrice));
        } else if (selectedSort.equals("GIÁ: CAO ĐẾN THẤP")) {
            filteredItems.sort((a, b) -> Double.compare(b.getStartingPrice(), a.getStartingPrice()));
        } else if (selectedSort.equals("KẾT THÚC SỚM NHẤT")) {
            filteredItems.sort((a, b) -> {
                Auction auctionA = itemAuctionMap.get(a.getId());
                Auction auctionB = itemAuctionMap.get(b.getId());
                LocalDateTime timeA = (auctionA != null && auctionA.getEndTime() != null) ? auctionA.getEndTime() : LocalDateTime.MAX;
                LocalDateTime timeB = (auctionB != null && auctionB.getEndTime() != null) ? auctionB.getEndTime() : LocalDateTime.MAX;
                return timeA.compareTo(timeB);
            });
        } else {
            // Default "MỚI NHẤT": Sort by ID descending
            filteredItems.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        }

        renderProductCards(filteredItems);
    }

    /**
     * Vẽ và hiển thị danh sách các thẻ sản phẩm (Product Card) lên lưới contentGrid.
     * Hỗ trợ cài đặt các nút chức năng: xem đấu giá trực tiếp, chỉnh sửa thông tin sản phẩm,
     * và xóa sản phẩm (bao gồm giải phóng bộ nhớ ảnh vật lý trên đĩa cứng).
     *
     * @param items Danh sách các sản phẩm cần hiển thị
     */
    private void renderProductCards(List<Item> items) {
        contentGrid.getChildren().clear();
        CalculatorView.updateCount(totalProductsLabel, items.size());

        if (items.isEmpty()) {
            Label noProductLabel = new Label("Không tìm thấy sản phẩm nào khớp bộ lọc.");
            noProductLabel.setStyle("-fx-text-fill: #8c8c8c; -fx-font-size: 14px; -fx-font-style: italic;");
            contentGrid.add(noProductLabel, 0, 0);
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            VBox productCard = ProductCardFactory.createProductCard(item,
                    (itemData, cardNode) -> {
                        Auction auction = itemAuctionMap.get(itemData.getId());
                        if (auction != null) {
                            try {
                                Stage existingStage = LiveAuctionController.getOpenStage(auction.getId());
                                if (existingStage != null) {
                                    existingStage.toFront();
                                    existingStage.requestFocus();
                                    return;
                                }

                                FXMLLoader loader = GenerationSupport.openNewStage("liveAuction-view.fxml", "Phòng đấu giá #" + auction.getId() + " - " + auction.getItem().getName());
                                if (loader != null) {
                                    LiveAuctionController controller = loader.getController();
                                    controller.setAuctionAndUser(auction, user);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                NotificationController.showError("Lỗi mở cửa sổ", "Không thể mở trang đấu giá trực tiếp.");
                            }
                        } else {
                            NotificationController.showError("Thông báo", "Sản phẩm chưa đăng ký hoặc không có phiên đấu giá.");
                        }
                    },
                    (itemData, cardNode) -> {
                        handleStartEditProduct(itemData);
                    },
                    (itemData, cardNode) -> {
                        if (cardNode != null) {
                            javafx.concurrent.Task<Response> deleteTask = new javafx.concurrent.Task<>() {
                                @Override
                                protected Response call() throws Exception {
                                    return ServerConnection.getInstance().send(RequestType.DELETE_ITEM, itemData.getId());
                                }
                            };

                            deleteTask.setOnSucceeded(evt -> {
                                Response res = deleteTask.getValue();
                                if (res != null && res.isSuccess()) {
                                    // 1. Giải phóng Image ở ImageView trong cardNode để tránh file lock trên Windows
                                    ImageView imgView = (ImageView) cardNode.lookup("#productCardImageView");
                                    if (imgView != null) {
                                        imgView.setImage(null);
                                    }

                                    // 2. Nếu sản phẩm bị xóa trùng với sản phẩm đang hiển thị trong form sửa, giải phóng nó
                                    if (editingItem != null && editingItem.getId() == itemData.getId()) {
                                        productImageView.setImage(null);
                                        productImageView.setVisible(false);
                                        uploadPrompt.setVisible(true);
                                        selectedImageFile = null;
                                        linkImageUrl = null;
                                        editingItem = null;
                                    }

                                    // Ép Garbage Collector chạy để giải phóng tài nguyên hệ thống (file lock) lập tức
                                    System.gc();

                                    // 3. Thực hiện xóa file ảnh vật lý
                                    String imageUrl = itemData.getImageUrl();
                                    if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("/")) {
                                        try {
                                            java.io.File file = new java.io.File("src/main/resources" + imageUrl);
                                            if (file.exists()) {
                                                boolean isDeleted = file.delete();
                                                if (isDeleted) {
                                                    System.out.println("Đã xóa file ảnh vật lý thành công tại: " + file.getAbsolutePath());
                                                } else {
                                                    System.err.println("Không thể xóa file vật lý! Tệp tin vẫn đang bị lock hoặc không có quyền: " + file.getAbsolutePath());
                                                }
                                            }
                                        } catch (Exception ex) {
                                            System.err.println("Lỗi khi xóa file ảnh: " + ex.getMessage());
                                        }
                                    }

                                    NotificationController.showNotification("Thành công", "Đã xóa sản phẩm thành công!");
                                    this.contentGrid.getChildren().remove(cardNode);
                                    auctionItems.remove(itemData);
                                    applyFiltersAndSort();
                                } else {
                                    String errMsg = (res != null) ? res.getMessage() : "Lỗi không xác định";
                                    NotificationController.showError("Lỗi xóa sản phẩm", "Không thể xóa sản phẩm.\nChi tiết: " + errMsg);
                                }
                            });

                            deleteTask.setOnFailed(evt -> {
                                NotificationController.showError("Lỗi hệ thống", "Đã xảy ra lỗi kết nối khi gửi yêu cầu xóa.");
                            });

                            Thread t = new Thread(deleteTask);
                            t.setDaemon(true);
                            t.start();
                        }
                    }
            );

            int column = i % 3;
            int row = i / 3;
            contentGrid.add(productCard, column, row);
        }
    }

    /**
     * Xử lý sự kiện chuyển đổi giữa các Tab chức năng trên thanh Sidebar bên trái (My Listings, Analytics, Order History, Settings).
     * Sử dụng Helper để thay đổi trạng thái nổi bật của nút và hiển thị view tương ứng.
     *
     * @param event Sự kiện nhấp chuột từ người dùng
     */
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

    /**
     * Xử lý sự kiện khi người dùng bấm nút "Thêm sản phẩm" (AddItem).
     * Làm sạch biểu mẫu đăng ký, mở khóa các trường thông tin đấu giá và hiển thị giao diện đăng ký.
     *
     * @param event Sự kiện nhấn nút
     */
    @FXML
    void handleAddItem(ActionEvent event) {
        editingItem = null;
        clearRegistrationForm();

        // Kích hoạt lại các trường đấu giá
        minimumBidIncrementField.setDisable(false);
        startDatePicker.setDisable(false);
        endDatePicker.setDisable(false);

        // Đặt lại tiêu đề mặc định
        registerTitleLabel.setText("ĐĂNG KÝ ĐẤU GIÁ MỚI");
        registerSubtitleLabel.setText("Điền đầy đủ thông tin để niêm yết sản phẩm");
        submitButton.setText("XÁC NHẬN ĐĂNG KÝ");

        // Lưu lại vết trước khi sang trang đăng ký
        lastView = currentView;
        lastButton = SellerUIHelper.findActiveButton(allButtons);

        // Thay vì chuyển Scene, ta chuyển View trong cùng 1 cửa sổ
        SellerUIHelper.resetAllButtons(allButtons); // Bỏ active ở các nút sidebar
        currentView = registerProductView;
        SellerUIHelper.showView(registerProductView, allViews);
    }

    /**
     * Quay trở lại màn hình hiển thị trước đó từ biểu mẫu đăng ký/chỉnh sửa sản phẩm.
     * Cập nhật lại style của nút chức năng tương ứng trên Sidebar.
     *
     * @param event Sự kiện nhấn nút quay lại
     */
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

    /**
     * Tải danh sách sản phẩm cá nhân và thông tin các phiên đấu giá từ Server.
     * Thực hiện chạy tác vụ tải dữ liệu trong một luồng ngầm (Background Task) để không chặn luồng giao diện chính (UI Thread).
     */
    public void loadMyListingView() {
        // 1. ƯU TIÊN ỨNG DỤNG TRƯỚC: Xóa sạch lưới UI cũ và đưa số lượng về 0 ngay lập tức
        contentGrid.getChildren().clear();
        CalculatorView.updateCount(totalProductsLabel, 0);

        // Hiển thị một nhãn thông báo trạng thái đang tải tạm thời để người dùng không cảm thấy ứng dụng bị "đơ"
        Label loadingLabel = new Label("Đang kết nối máy chủ để tải danh sách...");
        loadingLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-style: italic;");
        contentGrid.add(loadingLabel, 0, 0);

        System.out.println("Đang gửi yêu cầu lấy danh sách sản phẩm cá nhân và phiên đấu giá từ Server ở Luồng Ngầm...");

        // 2. KHỞI TẠO TÁC VỤ CHẠY NGẦM (BACKGROUND TASK)
        Task<Map<String, Response>> fetchTask = new Task<>() {
            @Override
            protected Map<String, Response> call() throws Exception {
                Response itemsRes = ServerConnection.getInstance().send(RequestType.GET_MY_ITEMS, null);
                Response auctionsRes = ServerConnection.getInstance().send(RequestType.GET_AUCTIONS, null);
                Map<String, Response> map = new HashMap<>();
                map.put("items", itemsRes);
                map.put("auctions", auctionsRes);
                return map;
            }
        };

        // 3. XỬ LÝ KHI TẢI DỮ LIỆU THÀNH CÔNG (Tự động chuyển về luồng giao diện JavaFX)
        fetchTask.setOnSucceeded(event -> {
            contentGrid.getChildren().remove(loadingLabel); // Xóa bỏ dòng chữ "Đang tải"
            Map<String, Response> results = fetchTask.getValue();
            Response itemsResponse = results.get("items");
            Response auctionsResponse = results.get("auctions");

            if (itemsResponse != null && itemsResponse.isSuccess()) {
                auctionItems = (List<Item>) itemsResponse.getData();
                itemAuctionMap.clear();

                if (auctionsResponse != null && auctionsResponse.isSuccess()) {
                    List<Auction> auctions = (List<Auction>) auctionsResponse.getData();
                    if (auctions != null) {
                        for (Auction auction : auctions) {
                            if (auction.getItem() != null) {
                                itemAuctionMap.put(auction.getItem().getId(), auction);
                            }
                        }
                    }
                }

                // Thực hiện lọc, sắp xếp và render các card sản phẩm
                applyFiltersAndSort();
            } else {
                String errorMsg = (itemsResponse != null) ? itemsResponse.getMessage() : "Không thể kết nối tới máy chủ!";
                NotificationController.showError(
                        "Lỗi tải dữ liệu",
                        "Hệ thống gặp sự cố khi đồng bộ danh sách sản phẩm.\nChi tiết: " + errorMsg
                );
            }
        });

        // 4. XỬ LÝ KHI GẶP LỖI HỆ THỐNG TRONG LUỒNG NGẦM
        fetchTask.setOnFailed(e -> {
            contentGrid.getChildren().remove(loadingLabel);
            NotificationController.showError("Lỗi hệ thống", "Đã xảy ra lỗi bất ngờ khi tải dữ liệu từ luồng nền.");
        });

        // 5. KÍCH HOẠT VÀ CHẠY THREAD KHÔNG BLOCKING
        Thread thread = new Thread(fetchTask);
        thread.setDaemon(true); // Đảm bảo tiểu trình tự đóng khi ứng dụng tắt
        thread.start();
    }

    /**
     * Tải và tính toán dữ liệu thống kê doanh thu theo từng tháng và từng danh mục sản phẩm (Electronics, Art, Vehicle).
     * Cập nhật các chỉ số tổng hợp (Tổng doanh thu, số sản phẩm đã bán, tổng lượt đặt giá) lên giao diện
     * và vẽ biểu đồ hình cột thống kê doanh thu.
     */
    public void loadAnalyticsData() {
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

        // Khởi tạo 12 tháng với giá trị 0.0
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        Map<String, Double> totalMap = new LinkedHashMap<>();
        Map<String, Double> electronicsMap = new LinkedHashMap<>();
        Map<String, Double> artMap = new LinkedHashMap<>();
        Map<String, Double> vehicleMap = new LinkedHashMap<>();

        for (String m : months) {
            totalMap.put(m, 0.0);
            electronicsMap.put(m, 0.0);
            artMap.put(m, 0.0);
            vehicleMap.put(m, 0.0);
        }

        double totalRevenue = 0.0;
        int soldCount = 0;
        int totalBids = 0;

        if (auctionItems != null && itemAuctionMap != null) {
            for (Item item : auctionItems) {
                Auction auction = itemAuctionMap.get(item.getId());
                if (auction != null) {
                    // Sum the bids for all auctions of the seller's items
                    if (auction.getBidHistory() != null) {
                        totalBids += auction.getBidHistory().size();
                    }

                    // Check if the auction ended successfully (with a winner, and status is FINISHED or PAID)
                    if ((auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID)
                            && auction.getCurrentWinner() != null) {

                        soldCount++;
                        double price = auction.getCurrentHighestBid();
                        totalRevenue += price;

                        String monthAbbr = getMonthAbbreviation(auction.getEndTime());

                        // Add to total
                        totalMap.put(monthAbbr, totalMap.get(monthAbbr) + price);

                        // Add to category
                        String category = item.getCategory();
                        if (category != null) {
                            switch (category.toUpperCase()) {
                                case "ELECTRONICS":
                                    electronicsMap.put(monthAbbr, electronicsMap.get(monthAbbr) + price);
                                    break;
                                case "ART":
                                    artMap.put(monthAbbr, artMap.get(monthAbbr) + price);
                                    break;
                                case "VEHICLE":
                                    vehicleMap.put(monthAbbr, vehicleMap.get(monthAbbr) + price);
                                    break;
                            }
                        }
                    }
                }
            }
        }

        // Đưa dữ liệu đã tính toán vào các Series
        for (String m : months) {
            totalSeries.getData().add(new XYChart.Data<>(m, totalMap.get(m)));
            electronicsSeries.getData().add(new XYChart.Data<>(m, electronicsMap.get(m)));
            artSeries.getData().add(new XYChart.Data<>(m, artMap.get(m)));
            vehicleSeries.getData().add(new XYChart.Data<>(m, vehicleMap.get(m)));
        }

        // Cập nhật các Label thống kê lên giao diện
        CalculatorView.updateCurrency(totalRevenueLabel, totalRevenue);
        CalculatorView.updateCount(soldProductsLabel, soldCount);
        CalculatorView.updateCount(totalBidsLabel, totalBids);

        // 4. Đưa tất cả series vào biểu đồ
        revenueChart.getData().addAll(totalSeries, electronicsSeries, artSeries, vehicleSeries);
    }

    /**
     * Chuyển đổi một mốc thời gian thành ký tự viết tắt của tháng bằng tiếng Anh (ví dụ: Jan, Feb, Mar...).
     *
     * @param dateTime Thời điểm cần chuyển đổi
     * @return Chuỗi viết tắt 3 ký tự của tháng
     */
    private String getMonthAbbreviation(LocalDateTime dateTime) {
        if (dateTime == null) return "Jan";
        return switch (dateTime.getMonth()) {
            case JANUARY -> "Jan";
            case FEBRUARY -> "Feb";
            case MARCH -> "Mar";
            case APRIL -> "Apr";
            case MAY -> "May";
            case JUNE -> "Jun";
            case JULY -> "Jul";
            case AUGUST -> "Aug";
            case SEPTEMBER -> "Sep";
            case OCTOBER -> "Oct";
            case NOVEMBER -> "Nov";
            case DECEMBER -> "Dec";
        };
    }

    /**
     * Khởi tạo cấu trúc các cột của bảng Lịch sử đơn hàng và thiết lập hiển thị/tô màu.
     * Ánh xạ các thuộc tính của đơn hàng vào bảng và định dạng kiểu chữ, màu sắc theo trạng thái đơn hàng.
     */
    private void setupOrderTableColumns() {
        // Cài đặt các cell value factory để ánh xạ thuộc tính của Order vào từng cột
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("product"));
        colBuyer.setCellValueFactory(new PropertyValueFactory<>("buyer"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Định dạng và tạo kiểu cho cột Mã đơn hàng
        colOrderId.setCellFactory(column -> new TableCell<Order, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("#" + item);
                    setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold;"); // Màu xanh da trời nhạt
                }
            }
        });

        // Định dạng và tạo kiểu cho cột Tên sản phẩm
        colProduct.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #FFFFFF;"); // Chữ màu trắng cho tên sản phẩm
                }
            }
        });

        // Định dạng và tạo kiểu cho cột Người mua
        colBuyer.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #E2E8F0;"); // Chữ màu xám đá nhạt
                }
            }
        });

        // Định dạng tiền tệ và tạo kiểu cột Giá chốt (Màu vàng hoàng kim)
        colPrice.setCellFactory(column -> new TableCell<Order, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("$%,.2f", item));
                    setStyle("-fx-text-fill: #FFC107; -fx-font-weight: bold;"); // Màu vàng hoàng kim
                }
            }
        });

        // Định dạng cột Ngày hoàn thành (yyyy-MM-dd HH:mm:ss)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        colDate.setCellFactory(column -> new TableCell<Order, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.format(formatter));
                    setStyle("-fx-text-fill: #94A3B8;"); // Màu xám đá
                }
            }
        });

        // Tô màu nổi bật theo trạng thái giao dịch
        colStatus.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toUpperCase());
                    if ("PAID".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold;"); // Màu xanh lá lục bảo
                    } else if ("FINISHED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold;"); // Màu vàng
                    } else {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;"); // Màu đỏ (đã hủy hoặc khác)
                    }
                }
            }
        });
    }

    /**
     * Tải và hiển thị danh sách lịch sử đơn hàng của người bán.
     * Lọc lấy tất cả các phiên đấu giá thuộc về người bán có trạng thái FINISHED hoặc PAID và đã có người chiến thắng.
     * Đồng thời đăng ký sự kiện tìm kiếm thời gian thực theo mã đơn, tên sản phẩm, người mua hoặc trạng thái.
     */
    public void loadOrderHistory() {
        System.out.println("Đang tải dữ liệu lịch sử đơn hàng...");

        List<Order> orders = new ArrayList<>();
        if (auctionItems != null && itemAuctionMap != null) {
            for (Item item : auctionItems) {
                Auction auction = itemAuctionMap.get(item.getId());
                // Chỉ lấy các phiên đấu giá kết thúc thành công (FINISHED hoặc PAID) và có người thắng
                if (auction != null && (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID)
                        && auction.getCurrentWinner() != null) {
                    orders.add(new Order(
                            auction.getId(),
                            item.getName(),
                            auction.getCurrentWinner().getUsername(),
                            auction.getCurrentHighestBid(),
                            auction.getEndTime(),
                            auction.getStatus().name()
                    ));
                }
            }
        }

        ObservableList<Order> orderList = FXCollections.observableArrayList(orders);
        FilteredList<Order> filteredData = new FilteredList<>(orderList, p -> true);

        // Đăng ký bộ lọc tìm kiếm theo thời gian thực nếu ô nhập liệu tồn tại
        if (searchOrderField != null) {
            searchOrderField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(order -> {
                    // Nếu từ khóa trống, hiển thị toàn bộ đơn hàng
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    // Khớp theo mã đơn, tên sản phẩm, người mua hoặc trạng thái đơn hàng
                    if (String.valueOf(order.getId()).contains(lowerCaseFilter)) {
                        return true;
                    } else if (order.getProduct().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (order.getBuyer().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (order.getStatus().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }
                    return false;
                });
            });
        }
        orderTable.setItems(filteredData);
    }

    /**
     * Khởi tạo các danh mục sản phẩm (Electronics, Vehicle, Art) bên trong thực đơn thả xuống (MenuButton).
     * Gắn kèm logic thay đổi biểu mẫu nhập động tương ứng với từng danh mục được chọn.
     */
    private void setupCategoryMenuItems() {
        MenuItem menuItemElectronics = new MenuItem("Electronics");
        MenuItem menuItemVehicle = new MenuItem("Vehicle");
        MenuItem menuItemArt = new MenuItem("Art");

        // Gắn logic xử lý hiển thị nhãn & gợi ý nhập dựa trên file thiết kế của bạn
        menuItemElectronics.setOnAction(e -> handleCategorySelection("Electronics", "THƯƠNG HIỆU (BRAND)", "Ví dụ: ASUS, Apple, Samsung..."));
        menuItemVehicle.setOnAction(e -> handleCategorySelection("Vehicle", "NĂM SẢN XUẤT (YEAR)", "Ví dụ: 2024, 2025..."));
        menuItemArt.setOnAction(e -> handleCategorySelection("Art", "NGHỆ SĨ (ARTIST)", "Ví dụ: Leonardo da Vinci, Nguyễn Phan Chánh..."));

        // Nạp các MenuItem này vào trong MenuButton giao diện
        categoryMenuButton.getItems().setAll(menuItemElectronics, menuItemVehicle, menuItemArt);
    }


    /**
     * Xử lý thay đổi các trường nhập thuộc tính động của sản phẩm dựa trên danh mục được chọn.
     * Tạo thêm Label và TextField tùy chọn tương ứng (Thương hiệu cho Electronics, Nghệ sĩ cho Art, Năm sản xuất cho Vehicle).
     *
     * @param categoryName Tên danh mục được chọn
     * @param labelText    Tiêu đề hiển thị cho trường động
     * @param promptText   Gợi ý nhập liệu cho trường động
     */
    private void handleCategorySelection(String categoryName, String labelText, String promptText) {
        // 1. Cập nhật text hiển thị trên MenuButton
        categoryMenuButton.setText(categoryName);

        // 2. Xóa các thuộc tính cũ đang hiển thị (nếu có)
        if (dynamicAttributesContainer != null) {
            dynamicAttributesContainer.getChildren().clear();

            // 3. Tạo VBox nhỏ để bọc Label và TextField mới
            VBox fieldGroup = new VBox(5.0); // spacing = 5

            // 4. Tạo Label (Áp dụng đúng style thiết kế)
            Label dynamicLabel = new Label(labelText.toUpperCase());
            dynamicLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-weight: bold;");

            // 5. Tạo TextField (Áp dụng đúng style input fields dark-theme của hệ thống)
            TextField dynamicTextField = new TextField();
            dynamicTextField.setPromptText(promptText);
            dynamicTextField.setPrefHeight(45.0);
            dynamicTextField.setStyle("-fx-background-color: #0A192F; -fx-text-fill: white; -fx-border-color: #1E2D45; -fx-border-radius: 8; -fx-background-radius: 8;");

            // Đặt ID cố định để sau này dễ dàng gọi hàm .lookup() lấy dữ liệu ra
            dynamicTextField.setId("customAttributeField");

            // 6. Đưa Label và TextField vào nhóm, rồi đẩy vào container chính
            fieldGroup.getChildren().addAll(dynamicLabel, dynamicTextField);
            dynamicAttributesContainer.getChildren().add(fieldGroup);
        }
    }

    /**
     * Kích hoạt chế độ chỉnh sửa thông tin cho một sản phẩm cụ thể.
     * Đưa thông tin sản phẩm hiện tại vào form nhập, khóa các trường liên quan đến cấu hình phiên đấu giá
     * và đổi tiêu đề nút sang "LƯU THAY ĐỔI".
     *
     * @param item Đối tượng sản phẩm cần chỉnh sửa
     */
    private void handleStartEditProduct(Item item) {
        editingItem = item;

        // Điền dữ liệu vào form
        productNameField.setText(item.getName());
        startingPriceField.setText(String.valueOf(item.getStartingPrice()));
        descriptionArea.setText(item.getDescription());

        // Gán category
        String category = item.getCategory();
        categoryMenuButton.setText(category);

        // Hiển thị và điền thuộc tính động
        dynamicAttributesContainer.getChildren().clear();
        String labelText = "";
        String promptText = "";
        String attributeValue = "";

        if (item instanceof Art art) {
            labelText = "NGHỆ SĨ (ARTIST)";
            promptText = "Ví dụ: Leonardo da Vinci, Nguyễn Phan Chánh...";
            attributeValue = art.getArtist();
        } else if (item instanceof Electronics elec) {
            labelText = "THƯƠNG HIỆU (BRAND)";
            promptText = "Ví dụ: ASUS, Apple, Samsung...";
            attributeValue = elec.getBrand();
        } else if (item instanceof Vehicle veh) {
            labelText = "NĂM SẢN XUẤT (YEAR)";
            promptText = "Ví dụ: 2024, 2025...";
            attributeValue = String.valueOf(veh.getYear());
        }

        if (!labelText.isEmpty()) {
            handleCategorySelection(category, labelText, promptText);
            TextField customField = (TextField) dynamicAttributesContainer.lookup("#customAttributeField");
            if (customField != null) {
                customField.setText(attributeValue);
            }
        }

        // Vô hiệu hóa các trường liên quan đến đấu giá
        minimumBidIncrementField.setDisable(true);
        minimumBidIncrementField.setText("");
        startDatePicker.setDisable(true);
        startDatePicker.setValue(null);
        endDatePicker.setDisable(true);
        endDatePicker.setValue(null);

        // Hiển thị ảnh hiện tại
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            linkImageUrl = item.getImageUrl();
            selectedImageFile = null; // Chưa chọn ảnh mới

            try {
                java.io.File imgFile = new java.io.File("src/main/resources" + item.getImageUrl());
                if (imgFile.exists()) {
                    ImagesController.displayImage(imgFile, productImageView, uploadPrompt);
                } else {
                    productImageView.setImage(new Image("https://placehold.co/260x135/000000/FFFFFF/png?text=No+Image"));
                    productImageView.setVisible(true);
                    uploadPrompt.setVisible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            productImageView.setImage(null);
            productImageView.setVisible(false);
            uploadPrompt.setVisible(true);
            linkImageUrl = null;
            selectedImageFile = null;
        }

        // Đổi tiêu đề form và nút bấm
        registerTitleLabel.setText("SỬA THÔNG TIN SẢN PHẨM");
        registerSubtitleLabel.setText("Thay đổi thông tin cho sản phẩm ID: " + item.getId());
        submitButton.setText("LƯU THAY ĐỔI");

        // Chuyển sang view sửa (sử dụng chung giao diện đăng ký)
        lastView = currentView;
        lastButton = SellerUIHelper.findActiveButton(allButtons);
        SellerUIHelper.resetAllButtons(allButtons);
        currentView = registerProductView;
        SellerUIHelper.showView(registerProductView, allViews);
    }

    /**
     * Xử lý gửi biểu mẫu khi người dùng nhấn nút xác nhận (Lưu thay đổi hoặc Đăng ký sản phẩm mới).
     * Xác thực thông tin nhập liệu, đóng gói dữ liệu và gửi yêu cầu (CREATE_ITEM / UPDATE_ITEM) tới Server.
     * Nếu là sản phẩm mới, tự động gửi thêm yêu cầu tạo phiên đấu giá đi kèm (CREATE_AUCTION).
     *
     * @param event Sự kiện nhấn nút xác nhận
     */
    @FXML
    private void handleSubmitProduct(ActionEvent event) {
        if (editingItem != null) {
            // Chế độ chỉnh sửa sản phẩm
            if (!ProductRegistrationValidator.validateEdit(productNameField, categoryMenuButton, startingPriceField,
                    descriptionArea, (linkImageUrl != null && !linkImageUrl.isEmpty()), selectedImageFile, imageDropzone)) return;

            UpdateItemPayload updateItemPayload = new UpdateItemPayload();
            updateItemPayload.id = editingItem.getId();
            updateItemPayload.name = productNameField.getText().trim();
            updateItemPayload.type = categoryMenuButton.getText();
            updateItemPayload.startingPrice = Double.parseDouble(startingPriceField.getText());
            updateItemPayload.description = descriptionArea.getText().trim();
            updateItemPayload.imageUrl = linkImageUrl;

            // Lấy thuộc tính động
            if (dynamicAttributesContainer != null) {
                TextField customField = (TextField) dynamicAttributesContainer.lookup("#customAttributeField");
                if (customField != null && !customField.getText().trim().isEmpty()) {
                    String attributeValue = customField.getText().trim();
                    switch (updateItemPayload.type) {
                        case "Electronics":
                            updateItemPayload.brand = attributeValue;
                            break;
                        case "Art":
                            updateItemPayload.artist = attributeValue;
                            break;
                        case "Vehicle":
                            try {
                                updateItemPayload.year = Integer.parseInt(attributeValue);
                            } catch (NumberFormatException e) {
                                NotificationController.showNotification("Lỗi nhập liệu", "Năm sản xuất của phương tiện phải là một số nguyên hợp lệ!");
                                return;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }

            Response response = ServerConnection.getInstance().send(RequestType.UPDATE_ITEM, updateItemPayload);
            if (response != null && response.isSuccess()) {
                System.out.println("[DATABASE] Đã cập nhật sản phẩm thành công!");
                NotificationController.showNotification("Thành công", "Cập nhật sản phẩm thành công!");
                SellerUIHelper.setNeedsRefresh(true);
                clearRegistrationForm();
                editingItem = null;
                handleBackToListings(null);
                loadMyListingView();
            } else {
                String errorMsg = (response != null) ? response.getMessage() : "Không có kết nối với Server!";
                NotificationController.showError("Lỗi cập nhật", errorMsg);
            }

        } else {
            // Chế độ đăng ký sản phẩm mới
            if (!ProductRegistrationValidator.validateAll(productNameField, categoryMenuButton, startingPriceField,
                    minimumBidIncrementField, startDatePicker, endDatePicker,
                    descriptionArea, selectedImageFile, imageDropzone)) return;

            CreateItemPayload createItemPayload = new CreateItemPayload();
            createItemPayload.name = productNameField.getText().trim();
            createItemPayload.type = categoryMenuButton.getText();
            createItemPayload.startingPrice = Double.parseDouble(startingPriceField.getText());
            createItemPayload.description = descriptionArea.getText().trim();
            createItemPayload.imageUrl = linkImageUrl;

            // Lấy thuộc tính động
            if (dynamicAttributesContainer != null) {
                TextField customField = (TextField) dynamicAttributesContainer.lookup("#customAttributeField");
                if (customField != null && !customField.getText().trim().isEmpty()) {
                    String attributeValue = customField.getText().trim();
                    switch (createItemPayload.type) {
                        case "Electronics":
                            createItemPayload.brand = attributeValue;
                            break;
                        case "Art":
                            createItemPayload.artist = attributeValue;
                            break;
                        case "Vehicle":
                            try {
                                createItemPayload.year = Integer.parseInt(attributeValue);
                            } catch (NumberFormatException e) {
                                NotificationController.showNotification("Lỗi nhập liệu", "Năm sản xuất của phương tiện phải là một số nguyên hợp lệ!");
                                return;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }

            Response response = ServerConnection.getInstance().send(RequestType.CREATE_ITEM, createItemPayload);
            if (response != null && response.isSuccess()) {
                System.out.println("[DATABASE] Đã lưu sản phẩm vào cơ sở dữ liệu thành công!");

                Item createdItem = (Item) response.getData();
                if (createdItem != null) {
                    CreateAuctionPayload createAuctionPayload = new CreateAuctionPayload();
                    createAuctionPayload.itemId = createdItem.getId();

                    if (startDatePicker.getValue() != null) {
                        createAuctionPayload.startTime = startDatePicker.getValue().atStartOfDay();
                    } else {
                        createAuctionPayload.startTime = LocalDateTime.now();
                    }

                    if (endDatePicker.getValue() != null) {
                        createAuctionPayload.endTime = endDatePicker.getValue().atTime(23, 59, 59);
                    } else {
                        createAuctionPayload.endTime = LocalDateTime.now().plusDays(7);
                    }

                    try {
                        createAuctionPayload.minBidStep = Double.parseDouble(minimumBidIncrementField.getText().trim());
                    } catch (NumberFormatException e) {
                        createAuctionPayload.minBidStep = 1.0;
                    }

                    Response auctionResponse = ServerConnection.getInstance().send(RequestType.CREATE_AUCTION, createAuctionPayload);
                    if (auctionResponse != null && auctionResponse.isSuccess()) {
                        System.out.println("[DATABASE] Đã tạo phiên đấu giá thành công!");
                        NotificationController.showNotification("Thành công", "Đăng ký sản phẩm và tạo phiên đấu giá thành công!");
                    } else {
                        String auctionError = (auctionResponse != null) ? auctionResponse.getMessage() : "Lỗi kết nối khi tạo phiên đấu giá";
                        NotificationController.showError("Lỗi tạo phiên đấu giá", "Đã đăng ký sản phẩm nhưng không thể tạo phiên đấu giá.\nChi tiết: " + auctionError);
                    }
                } else {
                    NotificationController.showNotification("Thành công", "Đăng ký sản phẩm thành công!");
                }

                SellerUIHelper.setNeedsRefresh(true);
                clearRegistrationForm();
                handleBackToListings(null);
                loadMyListingView();
            } else {
                String errorMsg = (response != null) ? response.getMessage() : "Không có kết nối với Server!";
                NotificationController.showError("Lỗi đăng ký", errorMsg);
            }
        }
    }

    /**
     * Bật/Tắt (Ẩn/Hiện) trình đơn thả xuống hiển thị danh sách các thông báo của hệ thống.
     * Tự động ẩn danh sách thông tin tài khoản nếu nó đang mở để tránh chồng lấn giao diện.
     *
     * @param event Sự kiện nhấp nút thông báo
     */
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

    /**
     * Bật/Tắt (Ẩn/Hiện) trình đơn thả xuống chứa thông tin tài khoản cá nhân.
     * Tự động ẩn danh sách thông báo hệ thống nếu nó đang mở để tránh chồng lấn giao diện.
     *
     * @param event Sự kiện nhấp nút ảnh đại diện
     */
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

    /**
     * Xử lý sự kiện khi người dùng lựa chọn mục "Thông tin cá nhân".
     *
     * @param event Sự kiện nhấn nút
     */
    @FXML
    private void handleViewProfile(ActionEvent event) {
        System.out.println("Chuyển hướng đến trang Thông tin cá nhân...");
        // Thêm logic chuyển Tab hoặc mở Window mới tại đây
        profileDropdown.setVisible(false); // Ẩn menu đi sau khi chọn
        profileDropdown.setManaged(false);
    }

    /**
     * Xử lý sự kiện đăng xuất tài khoản.
     * Hiển thị hộp thoại xác nhận, dừng lắng nghe sự kiện từ Server, ngắt kết nối
     * và chuyển hướng người dùng quay trở lại màn hình đăng nhập.
     *
     * @param event Sự kiện click nút đăng xuất
     */
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
                LiveAuctionController.clearEnteredAuctions();
                // Gửi LOGOUT lên server TRƯỚC khi cắt kết nối để server dọn observer
                try {
                    ServerConnection.getInstance().send(RequestType.LOGOUT, null);
                } catch (Exception ignored) {}
                ServerConnection.getInstance().stopListening();
                ServerConnection.getInstance().disconnect();
                FXMLLoader loader = GenerationSupport.changeScene(event, "login-view.fxml", "Đăng nhập");

                if (loader != null) {
                    user = null;
                    LoginController controller = loader.getController();
                    // (Tùy chọn) hiển thị thông báo đăng xuất thành công bên màn hình Login
                } else {
                    System.err.println("Lỗi: Không thể tải giao diện login-view.fxml");
                }
//                } else {
//                    // Trường hợp Server trả về lỗi (Ví dụ: "Chưa đăng nhập" hoặc lỗi DB)
//                    String errorMsg = (response != null) ? response.getMessage() : "Không nhận được phản hồi từ Server.";
//                    NotificationController.showError("Lỗi đăng xuất", errorMsg);
//                }
            } catch (Exception e) {
                System.err.println("Lỗi kết nối khi đăng xuất: " + e.getMessage());
                NotificationController.showError("Lỗi kết nối", "Không thể kết nối tới Server để đăng xuất.");
            }
        }
    }

    /**
     * Xóa sạch dữ liệu và khôi phục trạng thái mặc định của biểu mẫu đăng ký sản phẩm.
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
        uploadPrompt.setVisible(true);// Hiện lại dòng chữ hướng dẫn "Bấm để chọn ảnh"
        linkImageUrl = null;
    }

    /**
     * Xử lý việc chọn ảnh cho sản phẩm.
     * Nếu chưa có ảnh, mở hộp thoại chọn tệp hình ảnh, sao chép file vào tài nguyên của dự án với tên ngẫu nhiên
     * và cập nhật đường dẫn ảnh tương đối phục vụ việc lưu trữ trên Server database.
     * Nếu đã có ảnh, yêu cầu người dùng xác nhận trước khi gỡ bỏ ảnh cũ.
     *
     * @param event Sự kiện chọn/xóa ảnh sản phẩm
     */
    @FXML
    private void handleSelectImage(Event event) {
        if (selectedImageFile == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn ảnh sản phẩm");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

            File file = fileChooser.showOpenDialog(imageDropzone.getScene().getWindow());
            if (file != null) {
                try {
                    // 1. Định nghĩa đường dẫn đến package lưu trữ trong src/main/resources
                    // Sử dụng "resources/uploads" để đảm bảo ứng dụng tìm thấy thư mục cục bộ
                    File packageDir = new File("src/main/resources/com/example/group11/ImageProduct");
                    if (!packageDir.exists()) {
                        packageDir.mkdirs(); // Tự động tạo thư mục/package nếu chưa có
                    }

                    // 2. Tạo tên file ngẫu nhiên bằng UUID để tránh trùng tên ảnh trong package
                    String extension = file.getName().substring(file.getName().lastIndexOf("."));
                    String uniqueFileName = UUID.randomUUID().toString() + extension;

                    // 3. Đường dẫn file đích nằm trong package hệ thống
                    File destFile = new File(packageDir, uniqueFileName);

                    // 4. Thực hiện copy file từ máy người dùng vào package của dự án
                    Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // 5. Gán giá trị file để quản lý UI lúc đó
                    selectedImageFile = destFile;

                    // ĐÂY CHÍNH LÀ CHUỖI STRING THUỘC TÍNH: Lưu đường dẫn tương đối trong package
                    // Chuỗi này sẽ được gửi lên database giống như thuộc tính Name, Desc...
                    linkImageUrl = "/com/example/group11/ImageProduct/" + uniqueFileName;

                    // 6. Hiển thị ảnh preview lên giao diện JavaFX
                    ImagesController.displayImage(destFile, productImageView, uploadPrompt);
                    System.out.println("Đã lưu ảnh. Đường dẫn DB: " + linkImageUrl);

                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationController.showError("Lỗi hệ thống", "Không thể copy và lưu file ảnh vào package!");
                }
            }
        } else {
            // Trường hợp đã có ảnh: Hỏi xác nhận xóa để chọn lại
            selectedImageFile = ImagesController.confirmRemoveImage(selectedImageFile, productImageView, uploadPrompt);
            if (selectedImageFile == null) {
                linkImageUrl = null; // Xóa chuỗi String đường dẫn nếu hủy chọn ảnh
            }
        }
    }
}