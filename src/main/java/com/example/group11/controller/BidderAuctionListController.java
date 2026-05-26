package com.example.group11.controller;

import static com.example.group11.controller.AuctionUIHelper.*;
import com.auction.client.ServerConnection;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.function.Consumer;

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
    private VBox mainDashboardView;

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
    private Label profileBalanceLabel;

    @FXML
    private Label profileWonCountLabel;

    @FXML
    private Label profileBidCountLabel;



    @FXML
    GridPane contentGrid;

    @FXML
    private VBox headerSection;

    @FXML
    private BorderPane mainPane;

    @FXML
    TextField searchBar;

    @FXML
    Label totalAuctionsLabel;

    @FXML
    Label walletBalance;

    @FXML
    private VBox profileDropdown;

    @FXML
    private VBox notificationDropdown;

    @FXML
    private VBox notificationListContainer;

    User user;
    List<Auction> allAuctions = new ArrayList<>();
    String currentTab = "DASHBOARD";
    private List<Button> allButtons;

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

    private int unreadNotificationsCount = 0;
    private Label badgeLabel;
    private StackPane badgeContainer;

    private final Consumer<Notification> realtimeListener = notification -> {
        Platform.runLater(() -> {
            System.out.println("Nhận thông báo realtime: " + notification.getType() + " - " + notification.getData());
            addNotificationToDropdown(notification);

            String type = notification.getType();

            if ("BID_UPDATE".equals(type)) {
                Object data = notification.getData();

                if (data instanceof BidUpdateData upd) {
                    // Cập nhật đúng card mà không reload toàn bộ
                    updateAuctionCardPrice(upd.auctionId, upd.newHighestBid,
                            upd.winnerUsername, upd.totalBids);
                } else {
                    // fallback nếu server cũ
                    loadAuctions();
                }

            } else if ("AUCTION_ENDED".equals(type)) {
                int aId = parseAuctionId(notification.getData());
                if (aId != -1) {
                    updateSingleCard(aId);
                } else {
                    loadAuctions();
                }
            } else if ("ITEM_STATUS_CHANGED".equals(type)) {
                try {
                    int itemId = Integer.parseInt(notification.getData().toString());
                    int aId = findAuctionIdByItemId(itemId);
                    if (aId != -1) {
                        updateSingleCard(aId);
                    } else {
                        loadAuctions();
                    }
                } catch (Exception e) {
                    loadAuctions();
                }
            } else if ("NEW_AUCTION".equals(type)) {
                loadAuctions();
                String text = notification.getData() != null ? notification.getData().toString() : "";
                NotificationController.showNotification("Sản phẩm mới!", text);
            } else if ("BALANCE_UPDATE".equals(type)) {
                if (user instanceof Bidder bidder) {
                    double newBalance = (Double) notification.getData();
                    bidder.setBalance(newBalance);
                    walletBalance.setText(String.format("%,.2f", newBalance));
                }
            } else if ("PAYMENT_SUCCESS".equals(type)) {
                String text = notification.getData() != null ? notification.getData().toString() : "";
                int aId = parseAuctionId(text);
                if (aId != -1) {
                    updateSingleCard(aId);
                } else {
                    loadAuctions();
                }
                NotificationController.showNotification("💰 Thanh toán thành công!", text);
            } else if ("PAYMENT_RECEIVED".equals(type)) {
                String text = notification.getData() != null ? notification.getData().toString() : "";
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


    /**
     * Thiết lập thông tin người dùng hiện tại (Bidder), cập nhật hiển thị số dư ví,
     * tải danh sách đấu giá ban đầu và đăng ký lắng nghe thông báo thời gian thực.
     *
     * @param user Đối tượng người dùng hiện tại đăng nhập hệ thống
     */
    public void setUser(User user) {
        this.user = user;
        if (user instanceof Bidder bidder) {
            walletBalance.setText(String.format("%,.2f", bidder.getBalance()));
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
        allButtons = List.of(btnDashboardS, btnMyBids, btnWatchlist, btnHistory, btnSettings);
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
        resetAllButtons(allButtons);
        setActiveStyle(clickedButton);

        currentTab = tabName;

        boolean isSettings = tabName.equals("SETTINGS");

        // 2. Xử lý hiển thị Header và ContentGrid tùy theo Tab
        headerSection.setVisible(!isSettings);
        headerSection.setManaged(!isSettings);
        contentGrid.setVisible(!isSettings);
        contentGrid.setManaged(!isSettings);

        VBox targetView = isSettings ? profileView : mainDashboardView;
        showView(targetView, List.of(mainDashboardView, profileView));

        if (isSettings && profileView != null) {
            // Rebuild profile view từ factory mỗi khi mở tab
            profileView.getChildren().clear();
            VBox builtProfile = ProfileViewFactory.create(user, msg ->
                NotificationController.showNotification("Đổi mật khẩu", msg)
            );
            profileView.getChildren().add(builtProfile);
        } else {
            applyFilters();
        }
        System.out.println("Bidder đang xem tab: " + tabName);
    }

    @FXML
    private void handleHammerPortalClick(MouseEvent event) {
        searchBar.clear();
        auctionStatus.setText("TRẠNG THÁI");
        auctionProduct.setText("SẢN PHẨM");
        resetAllButtons(allButtons);
        setActiveStyle(btnDashboardS);
        currentTab = "DASHBOARD";
        headerSection.setVisible(true);
        headerSection.setManaged(true);
        contentGrid.setVisible(true);
        contentGrid.setManaged(true);

        showView(mainDashboardView, List.of(mainDashboardView, profileView));

        loadAuctions();
        System.out.println("Hammer Portal clicked: reset to Dashboard and reloaded auctions.");
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

    void applyFilters() {
        if (allAuctions == null) return;
        String statusFilter = auctionStatus.getText().trim().toUpperCase();
        String categoryFilter = auctionProduct.getText().trim().toUpperCase();
        List<Auction> filtered = BidderFilterManager.filter(allAuctions, user, currentTab, searchBar.getText(), statusFilter, categoryFilter);
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
                    this::openLiveAuction,   // Hàm callback xử lý xem chi tiết hiện tại của bạn   // Hàm callback xử lý xem chi tiết hiện tại của bạn
                    isWatched,
                    this::handleToggleWatchlist,
                    this.user

            );
            productCard.setId("auction-card-" + auction.getId());

            int column = i % 3;
            int row = i / 3;
            contentGrid.add(productCard, column, row);
        }
    }

    private void openLiveAuction(Auction auction) {
        LiveAuctionNavigator.openLiveAuction(auction, user, this);
    }

    private void handleBid(Auction auction) {
        if (auction.getStatus() == AuctionStatus.FINISHED) {
            handleConfirmPayment(auction);
            return;
        }
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            NotificationController.showError("Lỗi đặt giá", "Phiên đấu giá không ở trạng thái đang diễn ra (RUNNING).");
            return;
        }

        double minAccepted = auction.getCurrentHighestBid() + auction.getMinBidStep();

        TextInputDialog dialog = new TextInputDialog(String.format("%.2f", minAccepted));
        dialog.setTitle("Đặt giá thầu");
        dialog.setHeaderText("Đấu giá cho sản phẩm: " + auction.getItem().getName());
        dialog.setContentText(String.format("Nhập số tiền đấu giá (Tối thiểu %,.2f $):", minAccepted));

        NotificationController.applyDarkTheme(dialog);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double bidAmount = Double.parseDouble(result.get());
                if (bidAmount < minAccepted) {
                    NotificationController.showError("Lỗi đặt giá",
                            String.format("Số tiền đặt giá phải tối thiểu %,.2f $", minAccepted));
                    return;
                }

                if (user instanceof Bidder bidder && bidder.getBalance() < bidAmount) {
                    NotificationController.showError("Lỗi đặt giá", "Số dư ví của bạn không đủ.");
                    return;
                }

                BidderActionService.placeBid(auction, bidAmount, user, new BidderActionService.ActionCallback() {
                    @Override
                    public void onSuccess(Response res) {
                        if (res != null && res.isSuccess()) {
                            NotificationController.showNotification("Thành công", "Đã đặt giá thầu thành công!");
                            if (user instanceof Bidder bidder) {
                                double newBalance = (Double) res.getData();
                                bidder.setBalance(newBalance);
                                bidder.getProfile().addParticipatedAuction(auction.getId());
                                walletBalance.setText(String.format("%,.2f", bidder.getBalance()));
                            }
                            loadAuctions();
                        } else {
                            String errMsg = (res != null) ? res.getMessage() : "Lỗi không xác định";
                            NotificationController.showError("Lỗi đặt giá", "Không thể đặt giá.\nChi tiết: " + errMsg);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        NotificationController.showError("Lỗi hệ thống", "Lỗi kết nối khi gửi yêu cầu đặt giá.");
                    }
                });

            } catch (NumberFormatException e) {
                NotificationController.showError("Lỗi định dạng", "Vui lòng nhập một số hợp lệ.");
            }
        }
    }

    private void handleConfirmPayment(Auction auction) {
        boolean confirm = NotificationController.showConfirmation(
                "Xác nhận thanh toán",
                "Bạn có chắc chắn muốn thanh toán cho sản phẩm: " + auction.getItem().getName() + "?",
                "Số tiền thanh toán: " + String.format("%,.2f $", auction.getCurrentHighestBid()),
                "Thanh toán",
                "Hủy"
        );

        if (confirm) {
            BidderActionService.confirmPayment(auction, new BidderActionService.ActionCallback() {
                @Override
                public void onSuccess(Response res) {
                    if (res != null && res.isSuccess()) {
                        NotificationController.showNotification("Thành công", "Đã xác nhận thanh toán thành công!");
                        loadAuctions();
                    } else {
                        String errMsg = (res != null) ? res.getMessage() : "Lỗi không xác định";
                        NotificationController.showError("Lỗi thanh toán", "Không thể thanh toán.\nChi tiết: " + errMsg);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    NotificationController.showError("Lỗi hệ thống", "Lỗi kết nối khi gửi yêu cầu thanh toán.");
                }
            });
        }
    }

    private void handleToggleWatchlist(Auction auction) {
        if (!(user instanceof Bidder bidder)) return;
        boolean isWatched = bidder.getProfile().getWatchlist().contains(auction.getId());

        BidderActionService.toggleWatchlist(auction, user, isWatched, new BidderActionService.ActionCallback() {
            @Override
            public void onSuccess(Response res) {
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
            }

            @Override
            public void onFailure(Throwable t) {
                NotificationController.showError("Lỗi hệ thống", "Lỗi kết nối khi gửi yêu cầu cập nhật danh sách theo dõi.");
            }
        });
    }

    /**
     * Đăng ký bộ xử lý nhận thông báo thời gian thực (realtime) từ máy chủ.
     * Cập nhật danh sách đấu giá hoặc hiển thị hộp thoại cảnh báo khi có sự thay đổi hoặc sự kiện đặc biệt.
     */
    private void setupRealtimeNotifications() {
        RealtimeNotificationService.setupRealtimeNotifications(realtimeListener);
    }
    /**
     * Cập nhật giá realtime trên card đấu giá đang hiển thị,
     * không cần reload lại toàn bộ danh sách auction.
     * Nếu chưa tìm thấy card thì tải lại danh sách.
     */

    private void updateAuctionCardPrice(int auctionId, double newPrice,
                                        String winner, int totalBids) {
        // Cập nhật object trong allAuctions list để filter không mất dữ liệu
        boolean found = false;
        for (Auction a : allAuctions) {
            if (a.getId() == auctionId) {
                // Cập nhật field trực tiếp (không gọi placeBid — tránh double-logic)
                a.restoreHighestBid(newPrice);
                found = true;
                break;
            }
        }
        if (found) {
            updateSingleCard(auctionId);
        } else {
            loadAuctions();
        }
    }

    private void updateSingleCard(int auctionId) {
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() {
                return ServerConnection.getInstance()
                        .send(RequestType.GET_AUCTION_DETAIL, new GetAuctionDetailPayload(auctionId, false));
            }
        };

        task.setOnSucceeded(evt -> {
            Response res = task.getValue();
            if (res != null && res.isSuccess() && res.getData() instanceof Auction detailed) {
                Platform.runLater(() -> {
                    // Cập nhật đối tượng trong allAuctions để khi lọc không bị mất dữ liệu
                    for (int i = 0; i < allAuctions.size(); i++) {
                        if (allAuctions.get(i).getId() == auctionId) {
                            allAuctions.set(i, detailed);
                            break;
                        }
                    }

                    // Tìm card trên giao diện để thay thế trực tiếp
                    Node oldCard = null;
                    for (Node node : contentGrid.getChildren()) {
                        if (("auction-card-" + auctionId).equals(node.getId())) {
                            oldCard = node;
                            break;
                        }
                    }
                    
                    if (oldCard != null) {
                        Integer col = javafx.scene.layout.GridPane.getColumnIndex(oldCard);
                        Integer row = javafx.scene.layout.GridPane.getRowIndex(oldCard);
                        
                        boolean isWatched = false;
                        if (user instanceof Bidder bidder) {
                            isWatched = bidder.getProfile().getWatchlist().contains(auctionId);
                        }

                        VBox newCard = AuctionCardFactory.createAuctionCard(
                                detailed,
                                this::handleBid,
                                this::openLiveAuction,
                                isWatched,
                                this::handleToggleWatchlist,
                                this.user
                        );
                        newCard.setId("auction-card-" + auctionId);

                        contentGrid.getChildren().remove(oldCard);
                        contentGrid.add(newCard, col != null ? col : 0, row != null ? row : 0);
                        System.out.println("Đã cập nhật riêng biệt card của phiên #" + auctionId);
                    } else {
                        // Card is not currently displayed (e.g. filtered out), no need to reload the whole screen
                        System.out.println("Card #" + auctionId + " không hiển thị, bỏ qua cập nhật UI.");
                    }
                });
            }
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private int parseAuctionId(Object data) {
        if (data == null) return -1;
        String text = data.toString();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("Phiên #(\\d+)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private int findAuctionIdByItemId(int itemId) {
        if (allAuctions == null) return -1;
        for (Auction a : allAuctions) {
            if (a.getItem() != null && a.getItem().getId() == itemId) {
                return a.getId();
            }
        }
        return -1;
    }

    /**
     * Thêm thông báo mới nhận được vào danh sách hiển thị của hộp thoại thông báo thả xuống (Dropdown).
     *
     * @param notification Đối tượng thông báo nhận được từ hệ thống
     */
    private String getFriendlyMessage(Notification notification) {
        return RealtimeNotificationService.getFriendlyMessage(notification);
    }

    private void setupNotificationBadge() {
        Label bellLabel = new Label("🔔");
        bellLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");

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

    private void addNotificationToDropdown(Notification notification) {
        if (notificationListContainer != null) {
            if ("BID_UPDATE".equals(notification.getType())) {
                return;
            }
            if (notification.getData() != null && "VIEW_UPDATE".equals(notification.getData().toString())) {
                return;
            }
            String friendlyMsg = getFriendlyMessage(notification);

            javafx.scene.layout.HBox notifItemBox = new javafx.scene.layout.HBox(8);
            notifItemBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            notifItemBox.setStyle("-fx-background-color: #1E2D45; -fx-padding: 8; -fx-background-radius: 4;");
            notifItemBox.setMaxWidth(220);

            Label lbl = new Label(friendlyMsg);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
            javafx.scene.layout.HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);
            lbl.setMaxWidth(180);

            Label btnDelete = new Label("✕");
            btnDelete.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");
            btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;"));
            btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;"));
            btnDelete.setOnMouseClicked(e -> {
                notificationListContainer.getChildren().remove(notifItemBox);
            });

            notifItemBox.getChildren().addAll(lbl, btnDelete);

            if ("AUCTION_WON".equals(notification.getType())) {
                notifItemBox.setStyle(notifItemBox.getStyle() + " -fx-border-color: #10B981; -fx-border-width: 1; -fx-border-radius: 4;");
                lbl.setStyle(lbl.getStyle() + " -fx-cursor: hand;");
                lbl.setOnMouseClicked(e -> {
                    // Chuyển tới tab HISTORY
                    btnHistory.fire();
                    // Đóng dropdown thông báo
                    notificationDropdown.setVisible(false);
                    notificationDropdown.setManaged(false);
                });
            }

            notificationListContainer.getChildren().add(0, notifItemBox);
        }
    }

    /**
     * Hiển thị hộp thoại cho phép người dùng nhập số tiền để nạp vào ví tài khoản.
     *
     * @param event Sự kiện hành động của JavaFX
     */

    @FXML
    private void handleAddFunds(ActionEvent event) {
        if (!TopUpService.checkTopUpLimit(user)) {
            if (user instanceof Bidder bidder && bidder.getLastTopUpTime() != null) {
                java.time.Duration diff = java.time.Duration.between(java.time.LocalDateTime.now(), bidder.getLastTopUpTime().plusHours(24));
                long hours = diff.toHours();
                long mins = diff.toMinutesPart();
                NotificationController.showAlert("Thông báo", String.format("Bạn chỉ được nạp tiền 1 lần mỗi 24 giờ. Vui lòng thử lại sau %d giờ %d phút.", hours, mins));
            } else {
                NotificationController.showAlert("Thông báo", "Bạn chỉ được nạp tiền 1 lần mỗi 24 giờ.");
            }
            return;
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
    private void handleSelectPackage(MouseEvent event) {
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
        for (Node child : clickedNode.getChildren()) {
            if (child instanceof Label) {
                Label lbl = (Label) child;
                if (lbl.getText().contains("đ") || lbl.getText().contains("$")) {
                    packageLabel = lbl.getText();
                    break;
                }
            }
        }
        if (packageLabel.isEmpty()) {
            packageLabel = String.format("$%,.0f", amount);
        }

        String finalPackageLabel = packageLabel;
        boolean confirm = NotificationController.showConfirmation(
                "Xác nhận nạp tiền",
                "Xác nhận nạp gói " + packageLabel + " vào tài khoản?",
                "Sau khi xác nhận, bạn sẽ không thể nạp thêm tiền nữa.",
                "Đồng ý",
                "Hủy"
        );

        if (confirm) {
            TopUpService.topUp(amount, user, new TopUpService.TopUpCallback() {
                @Override
                public void onSuccess(double newBalance) {
                    walletBalance.setText(String.format("%,.2f", newBalance));
                    if (user instanceof Bidder bidder) {
                        toppedUpBidders.add(bidder.getId());
                    }
                    addFundsBtn.setDisable(true);
                    NotificationController.showNotification("Thành công",
                            String.format("Đã nạp thành công %s vào ví. Cổng nạp tiền hiện tại đã được khóa.", finalPackageLabel));
                    addFundsOverlay.setVisible(false);
                    addFundsOverlay.setManaged(false);
                }

                @Override
                public void onFailure(String message) {
                    NotificationController.showError("Lỗi nạp tiền", message);
                    resetPackageStyles();
                    selectedPackageNode = null;
                }
            });
        } else {
            resetPackageStyles();
            selectedPackageNode = null;
        }
    }

    private void resetPackageStyles() {
        VBox[] packages = {package1, package2, package3, package4, package5, package6};
        for (VBox pkg : packages) {
            if (pkg != null) {
                pkg.setStyle("-fx-background-color: #1E2D45; -fx-border-color: #334155; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
                for (Node child : pkg.getChildren()) {
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
            for (Node child : pkg.getChildren()) {
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

        if (!isCurrentlyVisible) {
            unreadNotificationsCount = 0;
            updateNotificationBadge(0);
        }
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        System.out.println("Chuyển hướng đến trang Thông tin cá nhân...");
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
        handleSwitchTab(new ActionEvent(btnSettings, null));
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

    public void loadProfileData() {
        if (profileView == null || user == null) return;
        profileView.getChildren().clear();
        VBox builtProfile = ProfileViewFactory.create(user, msg ->
            NotificationController.showNotification("Đổi mật khẩu", msg)
        );
        profileView.getChildren().add(builtProfile);
    }

    public void setupRealtimeNotificationsPublic() {
        setupRealtimeNotifications();
    }
}