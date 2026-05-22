package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.Notification;
import com.auction.network.PlaceBidPayload;
import com.auction.network.RequestType;
import com.auction.network.Response;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Bộ điều khiển danh sách đấu giá dành cho người tham gia đấu giá (Bidder Auction List Controller).
 * Quản lý giao diện chính của người tham gia đấu giá bao gồm: hiển thị danh sách đấu giá,
 * lọc phiên đấu giá, theo dõi (Watchlist), đấu giá (My Bids), lịch sử đấu giá (History),
 * nạp tiền vào tài khoản và nhận thông báo thời gian thực (realtime).
 */
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
    private List<Auction> allAuctions = new ArrayList<>();
    private String currentTab = "DASHBOARD";

    @FXML
    private StackPane addFundsOverlay;

    @FXML
    private VBox package1;

    @FXML
    private VBox package2;

    @FXML
    private VBox package3;

    @FXML
    private VBox package4;

    @FXML
    private VBox package5;

    @FXML
    private VBox package6;

    private VBox selectedPackageNode = null;
    private static final java.util.Set<Integer> toppedUpBidders = new java.util.HashSet<>();

    /**
     * Thiết lập thông tin người dùng hiện tại (Bidder), cập nhật hiển thị số dư ví,
     * tải danh sách đấu giá ban đầu và đăng ký lắng nghe thông báo thời gian thực.
     *
     * @param user Đối tượng người dùng hiện tại đăng nhập hệ thống
     */
    public void setUser(User user) {
        this.user = user;
        if (user instanceof Bidder bidder) {
            walletBalance.setText(String.format("%.2f", bidder.getBalance()));
            if (toppedUpBidders.contains(bidder.getId())) {
                addFundsBtn.setDisable(true);
            }
        }
        loadAuctions();
        setupRealtimeNotifications();
    }

    /**
     * Khởi tạo các giá trị, định dạng giao diện ban đầu và đăng ký các bộ lắng nghe
     * sự kiện cho các nút điều khiển, ô tìm kiếm và nạp tiền.
     *
     * @param location Vị trí tương đối của file FXML nguồn
     * @param resources Bộ tài nguyên dùng để bản địa hóa đối tượng
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mặc định khi mở lên, Dashboard sẽ ở trạng thái Active
        setActiveStyle(btnDashboardS);

        // Xử lý cho MenuButton Trạng thái
        setupMenuButtonUpdate(auctionStatus);

        // Xử lý cho MenuButton Sản phẩm
        setupMenuButtonUpdate(auctionProduct);

        // Đăng ký sự kiện nút TẤT CẢ
        allList.setOnAction(event -> {
            searchBar.clear();
            auctionStatus.setText("TRẠNG THÁI");
            auctionProduct.setText("SẢN PHẨM");
            applyFilters();
        });

        // Đăng ký sự kiện ô tìm kiếm
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        // Đăng ký sự kiện nạp tiền
        addFundsBtn.setOnAction(event -> {
            handleAddFunds(null);
        });

        // Đăng ký sự kiện rê chuột (hover) cho các gói nạp
        VBox[] packages = {package1, package2, package3, package4, package5, package6};
        for (VBox pkg : packages) {
            if (pkg != null) {
                pkg.setOnMouseEntered(e -> {
                    if (pkg != selectedPackageNode) {
                        pkg.setStyle("-fx-background-color: #1E293B; -fx-border-color: #FFD700; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
                    }
                });
                pkg.setOnMouseExited(e -> {
                    if (pkg != selectedPackageNode) {
                        pkg.setStyle("-fx-background-color: #1E2D45; -fx-border-color: #334155; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
                    }
                });
            }
        }
    }

    /**
     * Tự động cập nhật nhãn (Text) hiển thị của MenuButton tương ứng khi người dùng
     * lựa chọn một MenuItem bộ lọc con bên trong, đồng thời áp dụng lại bộ lọc.
     *
     * @param menuButton Nút MenuButton cần thiết lập cập nhật
     */
    private void setupMenuButtonUpdate(MenuButton menuButton) {
        for (MenuItem item : menuButton.getItems()) {
            item.setOnAction(event -> {
                menuButton.setText(item.getText().toUpperCase());
                System.out.println("Bidder đang lọc theo: " + item.getText());
                applyFilters();
            });
        }
    }

    /**
     * Xử lý chuyển đổi giữa các tab chức năng trên thanh Sidebar (Dashboard, My Bids, Watchlist, History, Settings).
     * Làm nổi bật nút được chọn, cập nhật giao diện hiển thị và áp dụng bộ lọc dữ liệu tương ứng.
     *
     * @param event Sự kiện hành động của JavaFX
     */
    @FXML
    void handleSwitchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String tabName = clickedButton.getText().trim().toUpperCase();

        // 1. Reset màu sắc của tất cả nút Sidebar và highlight nút vừa bấm
        resetAllButtons();
        setActiveStyle(clickedButton);

        currentTab = tabName;

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
        }

        applyFilters();
        System.out.println("Bidder đang xem tab: " + tabName);
    }

    /**
     * Gửi yêu cầu bất đồng bộ (Task) lên máy chủ để tải danh sách toàn bộ các phiên đấu giá.
     * Hiển thị trạng thái tải và thông báo lỗi nếu có sự cố kết nối.
     */
    private void loadAuctions() {
        Label loadingLabel = new Label("Đang kết nối máy chủ để tải danh sách...");
        loadingLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-style: italic;");
        contentGrid.getChildren().clear();
        contentGrid.add(loadingLabel, 0, 0);

        Task<Response> loadTask = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return ServerConnection.getInstance().send(RequestType.GET_AUCTIONS, null);
            }
        };

        loadTask.setOnSucceeded(event -> {
            contentGrid.getChildren().remove(loadingLabel);
            Response response = loadTask.getValue();
            if (response != null && response.isSuccess()) {
                List<Auction> fetched = (List<Auction>) response.getData();
                if (fetched != null) {
                    allAuctions = fetched;
                } else {
                    allAuctions = new ArrayList<>();
                }
                applyFilters();
            } else {
                String errMsg = (response != null) ? response.getMessage() : "Lỗi kết nối máy chủ";
                NotificationController.showError("Lỗi tải dữ liệu", "Không thể tải danh sách phiên đấu giá.\nChi tiết: " + errMsg);
            }
        });

        loadTask.setOnFailed(event -> {
            contentGrid.getChildren().remove(loadingLabel);
            NotificationController.showError("Lỗi hệ thống", "Lỗi kết nối khi tải danh sách đấu giá.");
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Áp dụng đồng thời các bộ lọc dữ liệu: từ khóa tìm kiếm, trạng thái phiên đấu giá,
     * danh mục sản phẩm và tab chức năng hiện tại để cập nhật danh sách hiển thị.
     */
    private void applyFilters() {
        if (allAuctions == null) return;

        String query = (searchBar.getText() != null) ? searchBar.getText().trim().toLowerCase() : "";
        String statusFilter = auctionStatus.getText().trim().toUpperCase();
        String categoryFilter = auctionProduct.getText().trim().toUpperCase();

        List<Auction> filtered = allAuctions.stream().filter(auction -> {
            // 1. Lọc theo Tab
            if (currentTab.equals("DASHBOARD")) {
                if (auction.getStatus() != AuctionStatus.RUNNING && auction.getStatus() != AuctionStatus.OPEN) {
                    return false;
                }
            } else if (currentTab.equals("MY BIDS")) {
                if (user instanceof Bidder bidder) {
                    boolean bidPlaced = bidder.getProfile().getParticipatedAuctions().contains(auction.getId()) ||
                            auction.getBidHistory().stream().anyMatch(tx -> tx.getBidderId() == bidder.getId());
                    if (!bidPlaced) return false;
                } else {
                    return false;
                }
            } else if (currentTab.equals("WATCHLIST")) {
                if (user instanceof Bidder bidder) {
                    List<Integer> wl = bidder.getProfile().getWatchlist();
                    if (wl == null || !wl.contains(auction.getId())) return false;
                } else {
                    return false;
                }
            } else if (currentTab.equals("HISTORY")) {
                if (auction.getStatus() != AuctionStatus.FINISHED 
                        && auction.getStatus() != AuctionStatus.CANCELED 
                        && auction.getStatus() != AuctionStatus.PAID) {
                    return false;
                }
                if (user instanceof Bidder bidder) {
                    boolean won = (auction.getCurrentWinner() != null && auction.getCurrentWinner().getId() == bidder.getId()) 
                            || bidder.getProfile().getWonAuctions().contains(auction.getId());
                    boolean participated = bidder.getProfile().getParticipatedAuctions().contains(auction.getId()) 
                            || auction.getBidHistory().stream().anyMatch(tx -> tx.getBidderId() == bidder.getId());
                    if (!won && !participated) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            // 2. Lọc theo trạng thái từ MenuButton
            if (!statusFilter.equals("TRẠNG THÁI") && !statusFilter.equals("TẤT CẢ")) {
                if (!auction.getStatus().toString().equals(statusFilter)) {
                    return false;
                }
            }

            // 3. Lọc theo danh mục từ MenuButton
            if (!categoryFilter.equals("SẢN PHẨM") && !categoryFilter.equals("TẤT CẢ")) {
                if (auction.getItem() == null || !auction.getItem().getCategory().toUpperCase().equals(categoryFilter)) {
                    return false;
                }
            }

            // 4. Lọc theo từ khóa tìm kiếm (ID, Tên, Mô tả)
            if (!query.isEmpty()) {
                String idStr = String.valueOf(auction.getId());
                String itemName = (auction.getItem() != null && auction.getItem().getName() != null) 
                        ? auction.getItem().getName().toLowerCase() : "";
                String itemDesc = (auction.getItem() != null && auction.getItem().getDescription() != null) 
                        ? auction.getItem().getDescription().toLowerCase() : "";

                if (!idStr.contains(query) && !itemName.contains(query) && !itemDesc.contains(query)) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());

        renderAuctionCards(filtered);
    }

    /**
     * Hiển thị danh sách các thẻ phiên đấu giá (Auction Card) lên vùng lưới (GridPane) của giao diện.
     * Sử dụng AuctionCardFactory để khởi tạo giao diện thẻ động cho từng phiên đấu giá.
     *
     * @param auctions Danh sách các phiên đấu giá đã qua bộ lọc cần kết xuất
     */
    private void renderAuctionCards(List<Auction> auctions) {
        contentGrid.getChildren().clear();
        totalAuctionsLabel.setText(String.valueOf(auctions.size()));

        if (auctions.isEmpty()) {
            Label noData = new Label("Không tìm thấy phiên đấu giá nào khớp bộ lọc.");
            noData.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-style: italic;");
            contentGrid.add(noData, 0, 0);
            return;
        }

        for (int i = 0; i < auctions.size(); i++) {
            Auction auction = auctions.get(i);
            boolean isWatched = false;
            if (user instanceof Bidder bidder) {
                isWatched = bidder.getProfile().getWatchlist().contains(auction.getId());
            }
            // THAY THẾ HOÀN TOÀN FXML LOADER BẰNG CLASS JAVA CARD FACTORY
            VBox productCard = AuctionCardFactory.createAuctionCard(
                    auction,
                    this::handleBid,          // Hàm callback xử lý đặt giá hiện tại của bạn
                    this::handleViewDetails,   // Hàm callback xử lý xem chi tiết hiện tại của bạn
                    isWatched,
                    this::handleToggleWatchlist,
                    this.user
            );

            int column = i % 3;
            int row = i / 3;
            contentGrid.add(productCard, column, row);
        }
    }

    /**
     * Xử lý sự kiện xem chi tiết phiên đấu giá. Chuyển hướng sang màn hình đấu giá trực tiếp (Live Auction).
     *
     * @param auction Phiên đấu giá cần xem chi tiết
     */
    private void handleViewDetails(Auction auction) {
        try {
            FXMLLoader loader = GenerationSupport.changeScene(contentGrid, "liveAuction-view.fxml", "Live Auction");
            if (loader != null) {
                LiveAuctionController controller = loader.getController();
                controller.setAuctionAndUser(auction, user);
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificationController.showError("Lỗi chuyển trang", "Không thể mở trang đấu giá trực tiếp.");
        }
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn đặt giá nhanh từ màn hình danh sách.
     * Hiển thị hộp thoại nhập số tiền, kiểm tra số dư ví và gửi yêu cầu đặt giá lên máy chủ.
     *
     * @param auction Phiên đấu giá muốn đặt giá thầu
     */
    private void handleBid(Auction auction) {
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            NotificationController.showError("Lỗi đặt giá", "Phiên đấu giá không ở trạng thái đang diễn ra (RUNNING).");
            return;
        }

        double minAccepted = auction.getCurrentHighestBid() + auction.getMinBidStep();
        
        TextInputDialog dialog = new TextInputDialog(String.format("%.2f", minAccepted));
        dialog.setTitle("Đặt giá thầu");
        dialog.setHeaderText("Đấu giá cho sản phẩm: " + auction.getItem().getName());
        dialog.setContentText(String.format("Nhập số tiền đấu giá (Tối thiểu %.2f $):", minAccepted));

        NotificationController.applyDarkTheme(dialog);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double bidAmount = Double.parseDouble(result.get());
                if (bidAmount < minAccepted) {
                    NotificationController.showError("Lỗi đặt giá", 
                            String.format("Số tiền đặt giá phải tối thiểu %.2f $", minAccepted));
                    return;
                }

                if (user instanceof Bidder bidder && bidder.getBalance() < bidAmount) {
                    NotificationController.showError("Lỗi đặt giá", "Số dư ví của bạn không đủ.");
                    return;
                }

                Task<Response> bidTask = new Task<>() {
                    @Override
                    protected Response call() throws Exception {
                        PlaceBidPayload payload = new PlaceBidPayload();
                        payload.auctionId = auction.getId();
                        payload.amount = bidAmount;
                        return ServerConnection.getInstance().send(RequestType.PLACE_BID, payload);
                    }
                };

                bidTask.setOnSucceeded(evt -> {
                    Response res = bidTask.getValue();
                    if (res != null && res.isSuccess()) {
                        NotificationController.showNotification("Thành công", "Đã đặt giá thầu thành công!");
                        if (user instanceof Bidder bidder) {
                            bidder.deduct(bidAmount);
                            bidder.getProfile().addParticipatedAuction(auction.getId());
                            walletBalance.setText(String.format("%.2f", bidder.getBalance()));
                        }
                        loadAuctions();
                    } else {
                        String errMsg = (res != null) ? res.getMessage() : "Lỗi không xác định";
                        NotificationController.showError("Lỗi đặt giá", "Không thể đặt giá.\nChi tiết: " + errMsg);
                    }
                });

                bidTask.setOnFailed(evt -> {
                    NotificationController.showError("Lỗi hệ thống", "Lỗi kết nối khi gửi yêu cầu đặt giá.");
                });

                Thread t = new Thread(bidTask);
                t.setDaemon(true);
                t.start();

            } catch (NumberFormatException e) {
                NotificationController.showError("Lỗi định dạng", "Vui lòng nhập một số hợp lệ.");
            }
        }
    }

    /**
     * Thực hiện thêm hoặc xóa một phiên đấu giá khỏi danh sách theo dõi (Watchlist) của người dùng.
     * Gửi yêu cầu tương ứng lên máy chủ và cập nhật lại giao diện.
     *
     * @param auction Phiên đấu giá cần thay đổi trạng thái theo dõi
     */
    private void handleToggleWatchlist(Auction auction) {
        if (!(user instanceof Bidder bidder)) return;
        boolean isWatched = bidder.getProfile().getWatchlist().contains(auction.getId());
        RequestType type = isWatched ? RequestType.REMOVE_FROM_WATCHLIST : RequestType.ADD_TO_WATCHLIST;

        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return ServerConnection.getInstance().send(type, auction.getId());
            }
        };

        task.setOnSucceeded(evt -> {
            Response res = task.getValue();
            if (res != null && res.isSuccess()) {
                if (isWatched) {
                    bidder.getProfile().removeFromWatchlist(auction.getId());
                    NotificationController.showNotification("Thành công", "Đã xóa khỏi danh sách theo dõi!");
                } else {
                    bidder.getProfile().addToWatchlist(auction.getId());
                    NotificationController.showNotification("Thành công", "Đã thêm vào danh sách theo dõi!");
                }
                applyFilters();
            } else {
                String errMsg = (res != null) ? res.getMessage() : "Lỗi không xác định";
                NotificationController.showError("Lỗi", "Không thể cập nhật danh sách theo dõi.\nChi tiết: " + errMsg);
            }
        });

        task.setOnFailed(evt -> {
            NotificationController.showError("Lỗi hệ thống", "Lỗi kết nối khi gửi yêu cầu cập nhật danh sách theo dõi.");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Đăng ký bộ xử lý nhận thông báo thời gian thực (realtime) từ máy chủ.
     * Cập nhật danh sách đấu giá hoặc hiển thị hộp thoại cảnh báo khi có sự thay đổi hoặc sự kiện đặc biệt.
     */
    private void setupRealtimeNotifications() {
        ServerConnection.getInstance().setNotificationHandler(notification -> {
            Platform.runLater(() -> {
                System.out.println("Nhận thông báo realtime: " + notification.getType() + " - " + notification.getData());
                addNotificationToDropdown(notification);
                
                if ("BID_UPDATE".equals(notification.getType()) 
                        || "AUCTION_ENDED".equals(notification.getType()) 
                        || "ITEM_STATUS_CHANGED".equals(notification.getType())) {
                    loadAuctions();
                } else if ("WATCHLIST_ENDING_SOON".equals(notification.getType())) {
                    NotificationController.showAlert("Sắp kết thúc!", notification.getData().toString());
                }
            });
        });
    }

    /**
     * Thêm thông báo mới nhận được vào danh sách hiển thị của hộp thoại thông báo thả xuống (Dropdown).
     *
     * @param notification Đối tượng thông báo nhận được từ hệ thống
     */
    private void addNotificationToDropdown(Notification notification) {
        if (notificationListContainer != null) {
            Label lbl = new Label(notification.getType() + ": " + notification.getData().toString());
            lbl.setWrapText(true);
            lbl.setStyle("-fx-text-fill: white; -fx-background-color: #1E2D45; -fx-padding: 8; -fx-background-radius: 4; -fx-font-size: 11px;");
            lbl.setMaxWidth(220);
            notificationListContainer.getChildren().add(0, lbl);
        }
    }

    /**
     * Hiển thị hộp thoại cho phép người dùng nhập số tiền để nạp vào ví tài khoản.
     *
     * @param event Sự kiện hành động của JavaFX
     */
    @FXML
    private void handleAddFunds(ActionEvent event) {
        if (user instanceof Bidder bidder) {
            if (toppedUpBidders.contains(bidder.getId())) {
                NotificationController.showAlert("Thông báo", "Bạn đã nạp tiền trước đó rồi, không thể nạp thêm!");
                return;
            }
        }

        // Đặt lại trạng thái các gói nạp
        resetPackageStyles();
        selectedPackageNode = null;

        addFundsOverlay.setVisible(true);
        addFundsOverlay.setManaged(true);
    }

    @FXML
    private void handleCloseAddFunds(ActionEvent event) {
        addFundsOverlay.setVisible(false);
        addFundsOverlay.setManaged(false);
    }

    @FXML
    private void handleSelectPackage(javafx.scene.input.MouseEvent event) {
        VBox clickedNode = (VBox) event.getSource();

        if (clickedNode == selectedPackageNode) {
            return;
        }

        resetPackageStyles();
        selectedPackageNode = clickedNode;
        highlightSelectedPackage(clickedNode);

        String amountStr = (String) clickedNode.getUserData();
        double amount = Double.parseDouble(amountStr);

        String packageLabel = "";
        for (javafx.scene.Node child : clickedNode.getChildren()) {
            if (child instanceof Label) {
                Label lbl = (Label) child;
                if (lbl.getText().contains("đ")) {
                    packageLabel = lbl.getText();
                    break;
                }
            }
        }
        if (packageLabel.isEmpty()) {
            packageLabel = String.format("%,.0fđ", amount);
        }

        boolean confirm = NotificationController.showConfirmation(
                "Xác nhận nạp tiền",
                "Xác nhận nạp gói " + packageLabel + " vào tài khoản?",
                "Sau khi xác nhận, bạn sẽ không thể nạp thêm tiền nữa.",
                "Đồng ý",
                "Hủy"
        );

        if (confirm) {
            if (user instanceof Bidder bidder) {
                // Gửi request nạp tiền lên server để lưu vào database
                Response res = ServerConnection.getInstance().send(RequestType.TOP_UP, amount);
                if (res != null && res.isSuccess()) {
                    double newBalance = (Double) res.getData();
                    // Cập nhật lại đối tượng bidder ở phía client
                    bidder.topUp(amount);
                    walletBalance.setText(String.format("%.2f", newBalance));

                    // Lưu trạng thái đã nạp tiền
                    toppedUpBidders.add(bidder.getId());

                    // Khóa nút cộng tiền
                    addFundsBtn.setDisable(true);

                    NotificationController.showNotification("Thành công",
                            String.format("Đã nạp thành công %s vào ví. Cổng nạp tiền hiện tại đã được khóa.", packageLabel));

                    // Ẩn bảng nạp tiền
                    addFundsOverlay.setVisible(false);
                    addFundsOverlay.setManaged(false);
                } else {
                    NotificationController.showError("Lỗi nạp tiền", res != null ? res.getMessage() : "Không thể thực hiện nạp tiền");
                    resetPackageStyles();
                    selectedPackageNode = null;
                }
            }
        } else {
            // Người dùng hủy, đặt lại trạng thái các gói nạp
            resetPackageStyles();
            selectedPackageNode = null;
        }
    }

    private void resetPackageStyles() {
        VBox[] packages = {package1, package2, package3, package4, package5, package6};
        for (VBox pkg : packages) {
            if (pkg != null) {
                pkg.setStyle("-fx-background-color: #1E2D45; -fx-border-color: #334155; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
                for (javafx.scene.Node child : pkg.getChildren()) {
                    if (child instanceof Label) {
                        Label lbl = (Label) child;
                        if (lbl.getText().contains("Ưu đãi")) {
                            lbl.setStyle("-fx-text-fill: #22C55E; -fx-font-size: 10;");
                        } else {
                            lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                        }
                    }
                }
            }
        }
    }

    private void highlightSelectedPackage(VBox pkg) {
        if (pkg != null) {
            pkg.setStyle("-fx-background-color: #FF2A54; -fx-border-color: #FFD700; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
            for (javafx.scene.Node child : pkg.getChildren()) {
                if (child instanceof Label) {
                    Label lbl = (Label) child;
                    if (lbl.getText().contains("Ưu đãi")) {
                        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 10;");
                    } else {
                        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    }
                }
            }
        }
    }

    /**
     * Ẩn/Hiện hộp thoại thông tin cá nhân thả xuống (Profile Dropdown) ở góc trên bên phải.
     * Đồng thời ẩn Dropdown thông báo nếu đang mở.
     *
     * @param event Sự kiện hành động của JavaFX
     */
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

    /**
     * Ẩn/Hiện hộp thoại danh sách thông báo thả xuống (Notification Dropdown).
     * Đồng thời ẩn Dropdown thông tin cá nhân nếu đang mở.
     *
     * @param event Sự kiện hành động của JavaFX
     */
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

    /**
     * Xử lý sự kiện khi người dùng nhấn xem trang thông tin hồ sơ cá nhân.
     *
     * @param event Sự kiện hành động của JavaFX
     */
    @FXML
    private void handleViewProfile(ActionEvent event) {
        System.out.println("Chuyển hướng đến trang Thông tin cá nhân...");
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    /**
     * Xử lý sự kiện đăng xuất tài khoản. Hiển thị hộp thoại xác nhận, ngắt kết nối
     * thời gian thực và chuyển hướng người dùng quay trở lại giao diện Đăng nhập.
     *
     * @param event Sự kiện hành động của JavaFX
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

    /**
     * Đưa tất cả các nút về trạng thái bình thường.
     */
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

    /**
     * Làm nổi bật nút đang được chọn (Active).
     *
     * @param button Nút cần làm nổi bật
     */
    private void setActiveStyle(Button button) {
        String activeStyle = "-fx-background-color: #112240; " +
                "-fx-text-fill: #FFD700; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: transparent #FFD700 transparent transparent; " +
                "-fx-border-width: 0 4 0 0;";
        button.setStyle(activeStyle);
    }
}
