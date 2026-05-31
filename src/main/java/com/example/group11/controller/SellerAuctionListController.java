package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.model.user.User;
import com.auction.network.*;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.group11.controller.AuctionUIHelper.*;

/**
 * Bộ điều khiển (Controller) cho màn hình danh sách sản phẩm và đấu giá của Người bán (Seller).
 * Quản lý các chức năng: hiển thị danh sách sản phẩm cá nhân, bộ lọc tìm kiếm,
 * biểu đồ phân tích doanh thu, lịch sử đơn hàng, đăng ký và sửa đổi sản phẩm đấu giá.
 */
public class SellerAuctionListController implements Initializable {
    @FXML
    private VBox auctionLive;

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
    private VBox profileView;

    @FXML
    private Label profileIdLabel;

    @FXML
    private Label profileUsernameLabel;

    @FXML
    private Label profileEmailLabel;

    @FXML
    private Label profileRoleLabel;

    @FXML
    private Label profileRevenueLabel;

    @FXML
    private Label profileItemsCountLabel;



    @FXML
    MenuButton categoryMenuButton;

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
    GridPane contentGrid;

    @FXML
    TextArea descriptionArea;

    @FXML
    DatePicker endDatePicker;

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
    TextField minimumBidIncrementField;

    @FXML
    VBox myListingsView;

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
    ImageView productImageView;

    @FXML
    TextField productNameField;

    @FXML
    private VBox profileDropdown;

    @FXML
    VBox registerProductView;

    @FXML
    private BarChart<String, Number> revenueChart;

    @FXML
    private Label soldProductsLabel;

    @FXML
    DatePicker startDatePicker;

    @FXML
    TextField startingPriceField;

    @FXML
    Button submitButton;

    @FXML
    Label registerTitleLabel;

    @FXML
    Label registerSubtitleLabel;

    Item editingItem = null;

    @FXML
    private Label totalBidsLabel;

    @FXML
    Label totalProductsLabel;

    @FXML
    private Label totalRevenueLabel;

    @FXML
    VBox uploadPrompt;

    @FXML
    private Label walletBalance;

    @FXML
    VBox dynamicAttributesContainer;

    VBox lastView;

    Button lastButton;

    VBox currentView; // Lưu view hiện tại để gán cho lastView khi chuyển tiếp

    File selectedImageFile;

    List<Button> allButtons;

    List<VBox> allViews;

    private Map<String, VBox> viewMapping;

    private User user;

    private int unreadNotificationsCount = 0;
    private Label badgeLabel;
    private StackPane badgeContainer;

    private final Consumer<Notification> realtimeListener = notification -> {
        Platform.runLater(() -> {
            addNotificationToUI(notification);

            String type = notification.getType();
            if ("BID_UPDATE".equals(type)) {
                Object data = notification.getData();
                if (data instanceof BidUpdateData upd) {
                    int itemId = findItemIdByAuctionId(upd.auctionId);
                    if (itemId != -1) {
                        updateSingleCardByItemId(itemId);
                    } else {
                        loadMyListingView();
                    }
                }
            } else if ("AUCTION_ENDED".equals(type)) {
                int aId = parseAuctionId(notification.getData());
                if (aId != -1) {
                    int itemId = findItemIdByAuctionId(aId);
                    if (itemId != -1) {
                        updateSingleCardByItemId(itemId);
                    } else {
                        loadMyListingView();
                    }
                } else {
                    loadMyListingView();
                }
            } else if ("ITEM_STATUS_CHANGED".equals(type)) {
                try {
                    int itemId = Integer.parseInt(notification.getData().toString());
                    updateSingleCardByItemId(itemId);
                } catch (Exception e) {
                    loadMyListingView();
                }
            } else if ("PAYMENT_RECEIVED".equals(type)) {
                String text = notification.getData() != null ? notification.getData().toString() : "";
                int aId = parseAuctionId(text);
                if (aId != -1) {
                    int itemId = findItemIdByAuctionId(aId);
                    if (itemId != -1) {
                        updateSingleCardByItemId(itemId);
                    } else {
                        loadMyListingView();
                    }
                } else {
                    loadMyListingView();
                }
                NotificationController.showNotification("💰 Đã nhận thanh toán!", text);
            }

            if (notificationDropdown != null && !notificationDropdown.isVisible()) {
                if ("BID_UPDATE".equals(type) || (notification.getData() != null && "VIEW_UPDATE".equals(notification.getData().toString()))) {
                    // ignore
                } else {
                    unreadNotificationsCount++;
                    updateNotificationBadge(unreadNotificationsCount);
                }
            }
        });
    };

    String linkImageUrl;


    // DANH SÁCH LƯU TRỮ SẢN PHẨM TẠM THỜI TRONG BỘ NHỚ
    List<Item> auctionItems = new ArrayList<>();
    Map<Integer, Auction> itemAuctionMap = new HashMap<>();

    /**
     * Thiết lập thông tin người dùng hiện tại (Người bán).
     *
     * @param user Đối tượng người dùng
     */
    public void setUser(User user) {
        this.user = user;
        if (user instanceof com.auction.model.user.Seller seller) {
            Platform.runLater(() -> {
                if (walletBalance != null) {
                    walletBalance.setText(String.format("%,.2f", seller.getRevenue()));
                }
            });
        }
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
        allViews = List.of(myListingsView, analyticsPane, orderHistoryView, registerProductView, profileView);
        viewMapping = Map.of(
                "btnMyListings", myListingsView,
                "btnAnalytics", analyticsPane,
                "btnOrderHistory", orderHistoryView,
                "btnSettings", profileView
        );

        // THIẾT LẬP GIAO DIỆN MẶC ĐỊNH (My Listings)
        currentView = myListingsView;

        // Sử dụng Helper để hiển thị và đổi màu nút
        AuctionUIHelper.showView(currentView, allViews);
        AuctionUIHelper.setActiveStyle(btnMyListings);

        // NẠP DỮ LIỆU BAN ĐẦU
        // Gọi executeTabLogic với ID của tab mặc định
        AuctionUIHelper.executeTabLogic("btnMyListings", contentGrid, this);

        setupFilters();
        setupOrderTableColumns();
        GenerationSupport.setupMenuButtonUpdate(categoryMenuButton);

        setupCategoryMenuItems();

        setupRealtimeNotifications();

        if (auctionLive != null) {
            auctionLive.setOnMouseEntered(e -> {
                auctionLive.setStyle("-fx-background-color: rgba(17, 34, 64, 0.85); -fx-background-radius: 12; -fx-cursor: hand;");
            });
            auctionLive.setOnMouseExited(e -> {
                auctionLive.setStyle("-fx-background-color: rgba(17, 34, 64, 0.5); -fx-background-radius: 12; -fx-cursor: hand;");
            });
        }
        setupNotificationBadge();
    }

    /**
     * Thiết lập kết nối nhận thông báo thời gian thực từ Server.
     * Xử lý tự động tải lại danh sách sản phẩm khi nhận thấy có phiên đấu giá kết thúc
     * hoặc trạng thái sản phẩm thay đổi.
     */
    private void setupRealtimeNotifications() {
        RealtimeNotificationService.setupRealtimeNotifications(realtimeListener);
    }


    /**
     * Thêm một thông báo mới vào giao diện danh sách thông báo thả xuống (Dropdown).
     * Hỗ trợ định dạng tiêu đề và mô tả chi tiết tương ứng với từng loại thông báo.
     *
     * @param notification Đối tượng chứa dữ liệu thông báo từ Server
     */
    private void addNotificationToUI(com.auction.network.Notification notification) {
        if (notificationListContainer == null) return;
        if ("BID_UPDATE".equals(notification.getType())) {
            return;
        }
        if (notification.getData() != null && "VIEW_UPDATE".equals(notification.getData().toString())) {
            return;
        }

        VBox notifBox = new VBox(4.0);
        notifBox.setStyle("-fx-background-color: #1E2D45; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titleLabel = new Label();
        titleLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        titleLabel.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 12.0));

        String type = notification.getType();
        String title = "🔔 Thông báo hệ thống";
        String desc = notification.getData() != null ? notification.getData().toString() : "";

        switch (type) {
            case "SELLER_FIRST_BID":
                title = "🎉 Lượt đặt giá đầu tiên (\"mở bát\")";
                break;
            case "SELLER_BID_SURGE":
                title = "🔥 Lượt đặt giá tăng vọt";
                break;
            case "SELLER_PRICE_MILESTONE":
                title = "📈 Mốc giá mới";
                break;
            case "SELLER_PAYMENT_RECEIVED":
                title = "💰 Đã nhận thanh toán";
                break;
            case "PAYMENT_RECEIVED":
                title = "💰 Đã nhận thanh toán từ người mua";
                break;
            case "SELLER_AUCTION_SUCCESS":
                title = "🏆 Đấu giá thành công";
                break;
            case "SELLER_AUCTION_FAILED":
                title = "❌ Đấu giá thất bại";
                break;
            case "PRODUCT_APPROVED":
                title = "✔️ Sản phẩm được phê duyệt";
                break;
            case "AUCTION_CREATED":
                title = "📅 Đăng ký đấu giá thành công";
                break;
            case "AUCTION_ENDED":
                title = "🏁 Phiên đấu giá kết thúc";
                break;
            case "ITEM_STATUS_CHANGED":
                title = "🔄 Trạng thái sản phẩm thay đổi";
                if (notification.getData() != null) {
                    desc = "Sản phẩm ID: " + notification.getData() + " đã cập nhật trạng thái mới.";
                }
                break;
            default:
                title = "🔔 [" + type + "] Thông báo";
                break;
        }

        titleLabel.setText(title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label("Vừa xong");
        timeLabel.setTextFill(javafx.scene.paint.Color.web("#94a3b8"));
        timeLabel.setFont(new Font(10.0));

        Label btnDelete = new Label("✕");
        btnDelete.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 0 0 8;");
        btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 0 0 8;"));
        btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 0 0 8;"));
        btnDelete.setOnMouseClicked(e -> {
            e.consume(); // ngăn sự kiện click lan truyền lên VBox
            notificationListContainer.getChildren().remove(notifBox);
        });

        header.getChildren().addAll(titleLabel, spacer, timeLabel, btnDelete);

        Label descLabel = new Label(desc);
        descLabel.setTextFill(javafx.scene.paint.Color.web("#94A3B8"));
        descLabel.setFont(new Font(11.0));
        descLabel.setWrapText(true);

        notifBox.getChildren().addAll(header, descLabel);

        // Nút bấm chuyển đến tab tương ứng
        if ("SELLER_PAYMENT_RECEIVED".equals(type) || "SELLER_AUCTION_SUCCESS".equals(type)) {
            notifBox.setStyle(notifBox.getStyle() + " -fx-border-color: #38BDF8; -fx-border-width: 1;");
            notifBox.setOnMouseClicked(e -> {
                btnOrderHistory.fire();
                notificationDropdown.setVisible(false);
                notificationDropdown.setManaged(false);
            });
        } else if ("PRODUCT_APPROVED".equals(type) || "AUCTION_CREATED".equals(type) || "ITEM_STATUS_CHANGED".equals(type)) {
            notifBox.setStyle(notifBox.getStyle() + " -fx-border-color: #ffd700; -fx-border-width: 1;");
            notifBox.setOnMouseClicked(e -> {
                btnMyListings.fire();
                notificationDropdown.setVisible(false);
                notificationDropdown.setManaged(false);
            });
        }

        // Đưa thông báo mới lên đầu danh sách
        notificationListContainer.getChildren().add(0, notifBox);

        // Chỉ giữ 20 thông báo gần nhất
        if (notificationListContainer.getChildren().size() > 20) {
            notificationListContainer.getChildren().remove(20, notificationListContainer.getChildren().size());
        }
    }

    private void setupNotificationBadge() {
        Label bellLabel = new Label("🔔");
        bellLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

        badgeLabel = new Label("0");
        badgeLabel.setStyle("-fx-background-color: #E11D48; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 1 4 1 4; -fx-min-width: 14px; -fx-alignment: center;");
        badgeLabel.setVisible(false);
        badgeLabel.setManaged(false);

        badgeContainer = new StackPane();
        badgeContainer.getChildren().addAll(bellLabel, badgeLabel);
        StackPane.setAlignment(badgeLabel, javafx.geometry.Pos.TOP_RIGHT);
        badgeLabel.setTranslateX(8);
        badgeLabel.setTranslateY(-6);

        SystemNotification.setGraphic(badgeContainer);
        SystemNotification.setText("");
    }

    private void updateNotificationBadge(int count) {
        if (badgeLabel != null) {
            if (count > 0) {
                badgeLabel.setText(String.valueOf(count));
                badgeLabel.setVisible(true);
                badgeLabel.setManaged(true);
            } else {
                badgeLabel.setVisible(false);
                badgeLabel.setManaged(false);
            }
        }
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

    void applyFiltersAndSort() {
        if (auctionItems == null) return;
        String query = filterSearchId.getText() != null ? filterSearchId.getText().trim().toLowerCase() : "";
        String selectedStatus = filterStatus.getText() != null ? filterStatus.getText().trim().toUpperCase() : "TRẠNG THÁI";
        String selectedSort = filterSort.getText() != null ? filterSort.getText().trim().toUpperCase() : "SẮP XẾP";
        List<Item> filteredItems = SellerFilterManager.filterAndSort(auctionItems, itemAuctionMap, query, selectedStatus, selectedSort);
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
            VBox productCard = createSingleProductCard(item);
            int column = i % 3;
            int row = i / 3;
            contentGrid.add(productCard, column, row);
        }
    }

    private VBox createSingleProductCard(Item item) {
        return ProductCardFactory.createProductCard(item, itemAuctionMap.get(item.getId()),
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
    }

    private int findItemIdByAuctionId(int auctionId) {
        for (Map.Entry<Integer, Auction> entry : itemAuctionMap.entrySet()) {
            if (entry.getValue().getId() == auctionId) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private int parseAuctionId(Object data) {
        if (data == null) return -1;
        String text = data.toString();
        Pattern p = java.util.regex.Pattern.compile("Phiên #(\\d+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private void updateSingleCardByItemId(int itemId) {
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                Map<String, Object> result = new HashMap<>();
                Response itemsRes = ServerConnection.getInstance().send(RequestType.GET_MY_ITEMS, null);
                Response auctionsRes = ServerConnection.getInstance().send(RequestType.GET_AUCTIONS, null);
                result.put("items", itemsRes);
                result.put("auctions", auctionsRes);
                return result;
            }
        };

        task.setOnSucceeded(evt -> {
            Map<String, Object> results = task.getValue();
            Response itemsResponse = (Response) results.get("items");
            Response auctionsResponse = (Response) results.get("auctions");

            if (itemsResponse != null && itemsResponse.isSuccess()) {
                List<Item> fetchedItems = (List<Item>) itemsResponse.getData();
                if (fetchedItems != null) {
                    this.auctionItems = fetchedItems;
                }

                this.itemAuctionMap.clear();
                if (auctionsResponse != null && auctionsResponse.isSuccess()) {
                    List<Auction> auctions = (List<Auction>) auctionsResponse.getData();
                    if (auctions != null) {
                        for (Auction a : auctions) {
                            if (a.getItem() != null) {
                                this.itemAuctionMap.put(a.getItem().getId(), a);
                            }
                        }
                    }
                }

                Platform.runLater(() -> {
                    Item updatedItem = null;
                    for (Item it : auctionItems) {
                        if (it.getId() == itemId) {
                            updatedItem = it;
                            break;
                        }
                    }

                    if (updatedItem != null) {
                        Node oldCard = null;
                        for (Node node : contentGrid.getChildren()) {
                            if (("product-card-" + itemId).equals(node.getId())) {
                                oldCard = node;
                                break;
                            }
                        }

                        if (oldCard != null) {
                            Integer col = GridPane.getColumnIndex(oldCard);
                            Integer row = GridPane.getRowIndex(oldCard);

                            VBox newCard = createSingleProductCard(updatedItem);
                            // Thay thế trực tiếp trong list — GridPane không bị re-layout
                            int idx = contentGrid.getChildren().indexOf(oldCard);
                            contentGrid.getChildren().set(idx, newCard);

                            // Giữ nguyên vị trí col/row
                            GridPane.setColumnIndex(newCard, col != null ? col : 0);
                            GridPane.setRowIndex(newCard, row != null ? row : 0);
                            System.out.println("Đã cập nhật riêng biệt card sản phẩm ID: " + itemId);
                        } else {
                            applyFiltersAndSort();
                        }
                    }
                    updateHeaderRevenue();
                });
            }
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
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
        VBox targetView = AuctionUIHelper.getVBoxFromId(buttonId, viewMapping);

        // Chỉ lưu nếu view hiện tại khác với view sắp tới (tránh lưu đè chính nó)
        if (currentView != targetView) {
            lastView = currentView;
            lastButton = AuctionUIHelper.findActiveButton(allButtons); // Hàm phụ để tìm nút đang sáng
        }

        // 1. Cập nhật UI Sidebar
        AuctionUIHelper.resetAllButtons(allButtons);
        AuctionUIHelper.setActiveStyle(clickedButton);

        // Cập nhật currentView và hiển thị
        currentView = targetView;
        AuctionUIHelper.showView(currentView, allViews);

        AuctionUIHelper.executeTabLogic(buttonId, contentGrid, this);
    }

    @FXML
    private void handleHammerPortalClick(Event event) {
        // Reset bộ lọc
        filterSearchId.clear();
        filterStatus.setText("TRẠNG THÁI");
        filterSort.setText("SẮP XẾP");

        VBox targetView = myListingsView;
        if (currentView != targetView) {
            lastView = currentView;
            lastButton = AuctionUIHelper.findActiveButton(allButtons);
        }

        AuctionUIHelper.resetAllButtons(allButtons);
        AuctionUIHelper.setActiveStyle(btnMyListings);

        currentView = targetView;
        AuctionUIHelper.showView(currentView, allViews);

        AuctionUIHelper.executeTabLogic("btnMyListings", contentGrid, this);
        System.out.println("Hammer Portal clicked: reset to My Listings and reloaded products.");
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
        lastButton = AuctionUIHelper.findActiveButton(allButtons);

        // Thay vì chuyển Scene, ta chuyển View trong cùng 1 cửa sổ
        AuctionUIHelper.resetAllButtons(allButtons); // Bỏ active ở các nút sidebar
        currentView = registerProductView;
        AuctionUIHelper.showView(registerProductView, allViews);
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
            AuctionUIHelper.resetAllButtons(allButtons);
            if (lastButton != null) {
                setActiveStyle(lastButton);
            }
            // 2. Quay lại view cũ
            AuctionUIHelper.showView(lastView, allViews);
            // 3. Cập nhật lại currentView
            currentView = lastView;
        }
    }

    public void loadMyListingView() {
        SellerMyListingLoader.loadMyListingView(contentGrid, totalProductsLabel, (fetchedItems, fetchedMap) -> {
            this.auctionItems = fetchedItems;
            this.itemAuctionMap.clear();
            this.itemAuctionMap.putAll(fetchedMap);
            applyFiltersAndSort();
            updateHeaderRevenue();
        });
    }

    /**
     * Tải và tính toán dữ liệu thống kê doanh thu theo từng tháng và từng danh mục sản phẩm (Electronics, Art, Vehicle).
     * Cập nhật các chỉ số tổng hợp (Tổng doanh thu, số sản phẩm đã bán, tổng lượt đặt giá) lên giao diện
     * và vẽ biểu đồ hình cột thống kê doanh thu.
     */
    public void loadAnalyticsData() {
        SellerAnalyticsCalculator.loadAnalyticsData(auctionItems, itemAuctionMap, revenueChart, totalRevenueLabel, soldProductsLabel, totalBidsLabel);
    }

    public void updateHeaderRevenue() {
        double totalPaidRevenue = 0.0;
        if (auctionItems != null && itemAuctionMap != null) {
            for (Item item : auctionItems) {
                Auction auction = itemAuctionMap.get(item.getId());
                if (auction != null && auction.getStatus() == AuctionStatus.PAID && auction.getCurrentWinner() != null) {
                    totalPaidRevenue += auction.getCurrentHighestBid();
                }
            }
        }
        if (walletBalance != null) {
            walletBalance.setText(String.format("%,.2f", totalPaidRevenue));
        }
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
        SellerOrderHistoryLoader.setupOrderTableColumns(colOrderId, colProduct, colBuyer, colPrice, colDate, colStatus);
    }

    /**
     * Tải và hiển thị danh sách lịch sử đơn hàng của người bán.
     * Lọc lấy tất cả các phiên đấu giá thuộc về người bán có trạng thái FINISHED hoặc PAID và đã có người chiến thắng.
     * Đồng thời đăng ký sự kiện tìm kiếm thời gian thực theo mã đơn, tên sản phẩm, người mua hoặc trạng thái.
     */
    public void loadOrderHistory() {
        System.out.println("Đang tải dữ liệu lịch sử đơn hàng...");
        SellerOrderHistoryLoader.loadOrderHistory(auctionItems, itemAuctionMap, orderTable, searchOrderField);
    }

    void setupCategoryMenuItems() {
        MenuItem menuItemElectronics = new MenuItem("Electronics");
        MenuItem menuItemVehicle = new MenuItem("Vehicle");
        MenuItem menuItemArt = new MenuItem("Art");

        menuItemElectronics.setOnAction(e -> handleCategorySelection("Electronics", "THƯƠNG HIỆU (BRAND)", "Ví dụ: ASUS, Apple, Samsung..."));
        menuItemVehicle.setOnAction(e -> handleCategorySelection("Vehicle", "NĂM SẢN XUẤT (YEAR)", "Ví dụ: 2024, 2025..."));
        menuItemArt.setOnAction(e -> handleCategorySelection("Art", "NGHỆ SĨ (ARTIST)", "Ví dụ: Leonardo da Vinci, Nguyễn Phan Chánh..."));

        categoryMenuButton.getItems().setAll(menuItemElectronics, menuItemVehicle, menuItemArt);
    }


    void handleCategorySelection(String categoryName, String labelText, String promptText) {
        categoryMenuButton.setText(categoryName);

        if (dynamicAttributesContainer != null) {
            dynamicAttributesContainer.getChildren().clear();

            VBox fieldGroup = new VBox(5.0);

            Label dynamicLabel = new Label(labelText.toUpperCase());
            dynamicLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-weight: bold;");

            TextField dynamicTextField = new TextField();
            dynamicTextField.setPromptText(promptText);
            dynamicTextField.setPrefHeight(45.0);
            dynamicTextField.setStyle("-fx-background-color: #0A192F; -fx-text-fill: white; -fx-border-color: #1E2D45; -fx-border-radius: 8; -fx-background-radius: 8;");
            dynamicTextField.setId("customAttributeField");

            fieldGroup.getChildren().addAll(dynamicLabel, dynamicTextField);
            dynamicAttributesContainer.getChildren().add(fieldGroup);
        }
    }

    void handleStartEditProduct(Item item) {
        editingItem = item;

        productNameField.setText(item.getName());
        startingPriceField.setText(String.valueOf(item.getStartingPrice()));
        descriptionArea.setText(item.getDescription());

        String category = item.getCategory();
        categoryMenuButton.setText(category);

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

        minimumBidIncrementField.setDisable(true);
        minimumBidIncrementField.setText("");
        startDatePicker.setDisable(true);
        startDatePicker.setValue(null);
        endDatePicker.setDisable(true);
        endDatePicker.setValue(null);

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            linkImageUrl = item.getImageUrl();
            selectedImageFile = null;

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

        registerTitleLabel.setText("SỬA THÔNG TIN SẢN PHẨM");
        registerSubtitleLabel.setText("Thay đổi thông tin cho sản phẩm ID: " + item.getId());
        submitButton.setText("LƯU THAY ĐỔI");

        lastView = currentView;
        lastButton = AuctionUIHelper.findActiveButton(allButtons);
        AuctionUIHelper.resetAllButtons(allButtons);
        currentView = registerProductView;
        AuctionUIHelper.showView(registerProductView, allViews);
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
                if (customField != null) {
                    String attributeValue = customField.getText().trim();
                    if (attributeValue.isEmpty()) {
                        NotificationController.showError("Lỗi nhập liệu", "Vui lòng nhập thuộc tính đặc trưng của danh mục (Nghệ sĩ, Thương hiệu, Năm)!");
                        return;
                    }
                    switch (updateItemPayload.type.toUpperCase()) {
                        case "ELECTRONICS":
                            updateItemPayload.brand = attributeValue;
                            break;
                        case "ART":
                            updateItemPayload.artist = attributeValue;
                            break;
                        case "VEHICLE":
                            try {
                                updateItemPayload.year = Integer.parseInt(attributeValue);
                            } catch (NumberFormatException e) {
                                NotificationController.showError("Lỗi nhập liệu", "Năm sản xuất của phương tiện phải là một số nguyên hợp lệ!");
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
                AuctionUIHelper.setNeedsRefresh(true);
                int updatedItemId = editingItem.getId();
                clearRegistrationForm();
                editingItem = null;
                handleBackToListings(null);
                updateSingleCardByItemId(updatedItemId);
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
                if (customField != null) {
                    String attributeValue = customField.getText().trim();
                    if (attributeValue.isEmpty()) {
                        NotificationController.showError("Lỗi nhập liệu", "Vui lòng nhập thuộc tính đặc trưng của danh mục (Nghệ sĩ, Thương hiệu, Năm)!");
                        return;
                    }
                    switch (createItemPayload.type.toUpperCase()) {
                        case "ELECTRONICS":
                            createItemPayload.brand = attributeValue;
                            break;
                        case "ART":
                            createItemPayload.artist = attributeValue;
                            break;
                        case "VEHICLE":
                            try {
                                createItemPayload.year = Integer.parseInt(attributeValue);
                            } catch (NumberFormatException e) {
                                NotificationController.showError("Lỗi nhập liệu", "Năm sản xuất của phương tiện phải là một số nguyên hợp lệ!");
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
                        if (startDatePicker.getValue().equals(LocalDate.now())) {
                            createAuctionPayload.startTime = LocalDateTime.now();
                        } else {
                            createAuctionPayload.startTime = startDatePicker.getValue().atStartOfDay();
                        }
                    } else {
                        createAuctionPayload.startTime = LocalDateTime.now();
                    }

                    if (endDatePicker.getValue() != null) {
                        createAuctionPayload.endTime = endDatePicker.getValue().atTime(23, 59, 59);
                    } else {
                        createAuctionPayload.endTime = LocalDateTime.now().plusDays(2);
                    }

                    try {
                        createAuctionPayload.minBidStep = Double.parseDouble(minimumBidIncrementField.getText().trim());
                    } catch (NumberFormatException e) {
                        createAuctionPayload.minBidStep = 1.0;
                    }

                    Response auctionResponse = ServerConnection.getInstance().send(RequestType.CREATE_AUCTION, createAuctionPayload);
                    if (auctionResponse != null && auctionResponse.isSuccess()) {
                        System.out.println("[DATABASE] Đã tạo phiên đấu giá thành công!");
                        NotificationController.showNotification("Thành công", "Đăng ký sản phẩm thành công! Đang chờ Admin duyệt phiên đấu giá.");
                    } else {
                        String auctionError = (auctionResponse != null) ? auctionResponse.getMessage() : "Lỗi kết nối khi tạo phiên đấu giá";
                        NotificationController.showError("Lỗi tạo phiên đấu giá", "Đã gửi yêu cầu đăng ký sản phẩm nhưng không thể tạo phiên đấu giá.\nChi tiết: " + auctionError);
                    }
                } else {
                    NotificationController.showNotification("Thành công", "Đăng ký sản phẩm thành công! Đang chờ Admin duyệt sản phẩm.");
                }

                AuctionUIHelper.setNeedsRefresh(true);
                clearRegistrationForm();
                handleBackToListings(null);
                loadMyListingView();
            } else {
                String errorMsg = (response != null) ? response.getMessage() : "Không có kết nối với Server!";
                NotificationController.showError("Lỗi đăng ký", errorMsg);
            }
        }
    }

    @FXML
    private void handleToggleNotification(ActionEvent event) {
        if (profileDropdown != null && profileDropdown.isVisible()) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }

        boolean isCurrentlyVisible = notificationDropdown.isVisible();
        notificationDropdown.setVisible(!isCurrentlyVisible);
        notificationDropdown.setManaged(!isCurrentlyVisible);

        if (!isCurrentlyVisible) {
            unreadNotificationsCount = 0;
            updateNotificationBadge(0);
        }
    }

    @FXML
    private void handleToggleProfile(ActionEvent event) {
        if (notificationDropdown.isVisible()) {
            notificationDropdown.setVisible(false);
            notificationDropdown.setManaged(false);
        }

        boolean isVisible = profileDropdown.isVisible();
        profileDropdown.setVisible(!isVisible);
        profileDropdown.setManaged(!isVisible);
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        System.out.println("Chuyển hướng đến trang Thông tin cá nhân...");
        profileDropdown.setVisible(false); // Ẩn menu đi sau khi chọn
        profileDropdown.setManaged(false);
        handleSwitchTab(new ActionEvent(btnSettings, null));
    }

    public void loadProfileData() {
        SellerProfileLoader.loadProfileData(user, profileView);
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
                SessionManager.logout(event, user);
                user = null;
            } catch (Exception e) {
                System.err.println("Lỗi kết nối khi đăng xuất: " + e.getMessage());
                NotificationController.showError("Lỗi kết nối", "Không thể kết nối tới Server để đăng xuất.");
            }
        }
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

    void clearRegistrationForm() {
        productNameField.clear();
        startingPriceField.clear();
        minimumBidIncrementField.clear();
        descriptionArea.clear();

        categoryMenuButton.setText("Chọn danh mục");

        startDatePicker.setValue(null);
        endDatePicker.setValue(null);

        selectedImageFile = null;
        productImageView.setImage(null);
        productImageView.setVisible(false);
        uploadPrompt.setVisible(true);
        linkImageUrl = null;
    }

}
