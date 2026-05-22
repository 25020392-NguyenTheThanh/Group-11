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
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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

    public void setUser(User user) {
        this.user = user;
        if (user instanceof Bidder bidder) {
            walletBalance.setText(String.format("%.2f", bidder.getBalance()));
        }
        loadAuctions();
        setupRealtimeNotifications();
    }

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
    }

    // Tự động cập nhật nhãn (Text) của MenuButton khi người dùng chọn một Item bên trong
    private void setupMenuButtonUpdate(MenuButton menuButton) {
        for (MenuItem item : menuButton.getItems()) {
            item.setOnAction(event -> {
                menuButton.setText(item.getText().toUpperCase());
                System.out.println("Bidder đang lọc theo: " + item.getText());
                applyFilters();
            });
        }
    }

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
                if (user != null) {
                    boolean bidPlaced = auction.getBidHistory().stream()
                            .anyMatch(tx -> tx.getBidderId() == user.getId());
                    if (!bidPlaced) return false;
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
            // THAY THẾ HOÀN TOÀN FXML LOADER BẰNG CLASS JAVA CARD FACTORY
            VBox productCard = AuctionCardFactory.createAuctionCard(
                    auction,
                    this::handleBid,          // Hàm callback xử lý đặt giá hiện tại của bạn
                    this::handleViewDetails   // Hàm callback xử lý xem chi tiết hiện tại của bạn
            );

            int column = i % 3;
            int row = i / 3;
            contentGrid.add(productCard, column, row);
        }
    }

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

    private void setupRealtimeNotifications() {
        ServerConnection.getInstance().setNotificationHandler(notification -> {
            Platform.runLater(() -> {
                System.out.println("Nhận thông báo realtime: " + notification.getType() + " - " + notification.getData());
                addNotificationToDropdown(notification);
                
                if ("BID_UPDATE".equals(notification.getType()) 
                        || "AUCTION_ENDED".equals(notification.getType()) 
                        || "ITEM_STATUS_CHANGED".equals(notification.getType())) {
                    loadAuctions();
                }
            });
        });
    }

    private void addNotificationToDropdown(Notification notification) {
        if (notificationListContainer != null) {
            Label lbl = new Label(notification.getType() + ": " + notification.getData().toString());
            lbl.setWrapText(true);
            lbl.setStyle("-fx-text-fill: white; -fx-background-color: #1E2D45; -fx-padding: 8; -fx-background-radius: 4; -fx-font-size: 11px;");
            lbl.setMaxWidth(220);
            notificationListContainer.getChildren().add(0, lbl);
        }
    }

    @FXML
    private void handleAddFunds(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("1000");
        dialog.setTitle("Nạp tiền vào tài khoản");
        dialog.setHeaderText("Nạp thêm tiền vào ví của bạn");
        dialog.setContentText("Nhập số tiền muốn nạp ($):");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #0A192F; -fx-text-fill: white;");
        dialogPane.lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: white;"));
        dialogPane.getButtonTypes().forEach(buttonType -> {
            Button btn = (Button) dialogPane.lookupButton(buttonType);
            if (btn != null) {
                btn.setStyle("-fx-background-color: #112240; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double amount = Double.parseDouble(result.get());
                if (amount <= 0) {
                    NotificationController.showError("Lỗi", "Số tiền phải lớn hơn 0.");
                    return;
                }
                if (user instanceof Bidder bidder) {
                    bidder.topUp(amount);
                    walletBalance.setText(String.format("%.2f", bidder.getBalance()));
                    NotificationController.showNotification("Thành công", String.format("Đã nạp thành công %.2f $ vào ví.", amount));
                }
            } catch (NumberFormatException e) {
                NotificationController.showError("Lỗi", "Số tiền không hợp lệ.");
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
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        System.out.println("Chuyển hướng đến trang Thông tin cá nhân...");
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
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
                System.out.println("Đang thực hiện gửi yêu cầu đăng xuất lên Server...");
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

    // Đưa tất cả các nút về trạng thái bình thường
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
                "-fx-text-fill: #FFD700; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: transparent #FFD700 transparent transparent; " +
                "-fx-border-width: 0 4 0 0;";
        button.setStyle(activeStyle);
    }
}
