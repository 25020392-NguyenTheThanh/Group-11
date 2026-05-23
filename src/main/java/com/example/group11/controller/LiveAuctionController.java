package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.GetAuctionDetailPayload;
import com.auction.network.PlaceBidPayload;
import com.auction.network.RequestType;
import com.auction.network.Response;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Bộ điều khiển phòng đấu giá trực tiếp (Live Auction Controller).
 * Quản lý giao diện phòng đấu giá thời gian thực cho từng sản phẩm cụ thể,
 * bao gồm hiển thị thông tin sản phẩm, đếm ngược thời gian, lịch sử đặt giá,
 * biểu đồ biến động giá, thực hiện đặt giá thầu (Confirm Bid), theo dõi phòng đấu giá,
 * và thống kê số lượng người xem trực tuyến.
 */
public class LiveAuctionController implements Initializable {

    @FXML private Button backButton;
    @FXML private ImageView productImageView;
    @FXML private Label productNameLabel;
    @FXML private Label productDescLabel;
    @FXML private Label statusLabel;
    @FXML private Label timerLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label bidCountLabel;
    @FXML private Label viewerCountLabel; // Nhãn hiển thị số người xem
    @FXML private TextField bidAmountField;
    @FXML private Button confirmBidButton;
    @FXML private VBox bidHistoryContainer;
    @FXML private VBox biddingVBox; // Container chứa bàn đặt giá thầu
    @FXML private Button closeButton; // Nút X thoát hẳn phòng

    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    private Auction auction;
    private User user;
    private Timeline countdownTimeline;
    private Button watchlistButton;

    private final Consumer<com.auction.network.Notification> realtimeListener = notification -> {
        Platform.runLater(() -> {
            System.out.println("LiveAuction nhận thông báo realtime: " + notification.getType() + " - " + notification.getData());
            if ("BID_UPDATE".equals(notification.getType())
                    || "AUCTION_ENDED".equals(notification.getType())
                    || "ITEM_STATUS_CHANGED".equals(notification.getType())) {
                refreshAuctionDetails(false);
            } else if ("BALANCE_UPDATE".equals(notification.getType())) {
                if (user instanceof Bidder bidder) {
                    double newBalance = (Double) notification.getData();
                    bidder.setBalance(newBalance);
                    System.out.println("Cập nhật số dư realtime cho " + bidder.getUsername() + ": " + newBalance);
                }
            } else if ("WATCHLIST_ENDING_SOON".equals(notification.getType())) {
                NotificationController.showAlert("Sắp kết thúc!", notification.getData().toString());
            }
        });
    };

    // Bộ sưu tập lưu trữ các ID phòng đấu giá đã được truy cập ở phiên làm việc này (tránh tăng view trùng lặp khi quay lại)
    private static final Set<Integer> enteredAuctionIds = new HashSet<>();

    // Bộ sưu tập lưu giữ các Stage của các phòng đấu giá đang mở song song
    private static final Map<Integer, Stage> openStages = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Chuẩn bị ban đầu
    }

    /**
     * Thiết lập thông tin phiên đấu giá và người dùng hiện tại tham gia phòng.
     * Cấu hình nút Theo dõi nếu là Bidder, lưu trữ thông tin Stage cửa sổ,
     * đồng bộ thông tin chi tiết phiên đấu giá ban đầu từ server và bắt đầu đếm ngược.
     *
     * @param auction Đối tượng phiên đấu giá hiện tại
     * @param user Đối tượng người dùng đang truy cập
     */
    public void setAuctionAndUser(Auction auction, User user) {
        this.auction = auction;
        this.user = user;

        if (user instanceof Bidder bidder) {
            Platform.runLater(() -> {
                if (backButton != null && backButton.getParent() instanceof HBox topBar) {
                    if (watchlistButton == null) {
                        watchlistButton = new Button();
                        int closeBtnIndex = topBar.getChildren().indexOf(closeButton);
                        if (closeBtnIndex >= 0) {
                            topBar.getChildren().add(closeBtnIndex, watchlistButton);
                        } else {
                            topBar.getChildren().add(watchlistButton);
                        }
                        watchlistButton.setOnAction(event -> handleToggleWatchlistInDetail());
                    }
                    boolean isWatched = bidder.getProfile().getWatchlist().contains(auction.getId());
                    updateWatchlistButton(watchlistButton, isWatched);
                }
            });
        }

        // Lưu thông tin Stage của cửa sổ mới mở vào openStages để quản lý song song
        Platform.runLater(() -> {
            if (backButton != null && backButton.getScene() != null) {
                Stage stage = (Stage) backButton.getScene().getWindow();
                if (stage != null) {
                    openStages.put(auction.getId(), stage);
                    stage.setOnCloseRequest(event -> {
                        cleanupAndClose(true);
                    });
                }
            }
        });

        // 1. Lấy thông tin đấu giá mới nhất từ Server để đồng bộ và đăng ký Observer
        // Chỉ tăng lượt xem nếu chưa ở trong phòng đấu giá này trước đó
        boolean isFirstEntry = !enteredAuctionIds.contains(auction.getId());
        if (isFirstEntry) {
            enteredAuctionIds.add(auction.getId());
        }
        refreshAuctionDetails(isFirstEntry);

        // 2. Lắng nghe cập nhật realtime
        setupRealtimeNotifications();

        // 3. Chạy luồng đếm ngược
        startCountdown();
    }

    /**
     * Lấy đối tượng Stage cửa sổ giao diện phòng đấu giá đang mở dựa trên mã phiên đấu giá.
     *
     * @param auctionId Mã phiên đấu giá
     * @return Đối tượng Stage tương ứng hoặc null nếu không tìm thấy
     */
    public static Stage getOpenStage(int auctionId) {
        return openStages.get(auctionId);
    }

    /**
     * Đóng tất cả các cửa sổ phòng đấu giá đang được mở song song trên hệ thống.
     */
    public static void closeAllOpenStages() {
        Platform.runLater(() -> {
            for (Stage stage : new java.util.ArrayList<>(openStages.values())) {
                stage.close();
            }
            openStages.clear();
        });
    }

    /**
     * Dọn sạch danh sách phòng đấu giá đã truy cập và đóng tất cả cửa sổ đang mở
     * (thường được gọi khi người dùng đăng xuất hoặc thay đổi tài khoản).
     */
    public static void clearEnteredAuctions() {
        enteredAuctionIds.clear();
        closeAllOpenStages();
    }

    /**
     * Khởi tạo các giá trị ban đầu cho bộ điều khiển giao diện phòng đấu giá trực tiếp.
     *
     * @param location Vị trí tương đối của file FXML nguồn
     * @param resources Bộ tài nguyên dùng để bản địa hóa đối tượng
     */

    /**
     * Bắt đầu luồng đếm ngược (Timeline) cập nhật thời gian còn lại của phiên đấu giá mỗi giây.
     */
    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateCountdown();
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    /**
     * Cập nhật nhãn hiển thị thời gian đếm ngược dựa trên trạng thái thực tế của phiên đấu giá.
     * Nếu thời gian kết thúc, dừng đếm ngược và đồng bộ lại chi tiết phiên đấu giá.
     */
    private void updateCountdown() {
        if (auction == null) return;
        LocalDateTime now = LocalDateTime.now();
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            java.time.Duration duration = java.time.Duration.between(now, auction.getEndTime());
            if (!duration.isNegative()) {
                long hours = duration.toHours();
                long minutes = duration.toMinutesPart();
                long seconds = duration.toSecondsPart();
                timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            } else {
                timerLabel.setText("00:00:00");
                countdownTimeline.stop();
                refreshAuctionDetails(false);
            }
        } else if (auction.getStatus() == AuctionStatus.OPEN) {
            timerLabel.setText("WAITING");
        } else {
            timerLabel.setText("ENDED");
        }
    }

    /**
     * Gửi yêu cầu bất đồng bộ lên máy chủ để tải và đồng bộ lại chi tiết phiên đấu giá mới nhất.
     *
     * @param incrementView true nếu muốn tăng lượt xem (khi mới vào phòng), false nếu chỉ cập nhật thông thường
     */
    private void refreshAuctionDetails(boolean incrementView) {
        if (auction == null) return;

        Task<Response> detailTask = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return ServerConnection.getInstance().send(RequestType.GET_AUCTION_DETAIL, new GetAuctionDetailPayload(auction.getId(), incrementView));
            }
        };

        detailTask.setOnSucceeded(evt -> {
            Response res = detailTask.getValue();
            if (res != null && res.isSuccess()) {
                Auction updated = (Auction) res.getData();
                if (updated != null) {
                    this.auction = updated;
                    updateUI();
                }
            }
        });

        Thread t = new Thread(detailTask);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Đăng ký bộ xử lý nhận thông báo thời gian thực (realtime) từ máy chủ khi có lượt đặt giá mới,
     * kết thúc phiên đấu giá hoặc thay đổi trạng thái sản phẩm để cập nhật tức thời phòng đấu giá.
     */
    private void setupRealtimeNotifications() {
        ServerConnection.getInstance().addNotificationHandler(realtimeListener);
    }

    /**
     * Cập nhật toàn bộ các thành phần hiển thị trên giao diện (tên sản phẩm, mô tả, trạng thái,
     * giá cao nhất, số lượt đặt giá, số người xem, ảnh đại diện và bảng đặt giá thầu của Bidder).
     */
    private void updateUI() {
        if (auction == null || auction.getItem() == null) return;
        Item item = auction.getItem();

        productNameLabel.setText(item.getName());
        productDescLabel.setText(item.getDescription());

        boolean isRunning = (auction.getStatus() == AuctionStatus.RUNNING);

        // Thiết lập trạng thái trực quan
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            statusLabel.setText("● TRỰC TIẾP");
            statusLabel.setStyle("-fx-text-fill: #00e475; -fx-background-color: #052e16; -fx-padding: 4 8; -fx-border-radius: 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");
        } else if (auction.getStatus() == AuctionStatus.OPEN) {
            statusLabel.setText("● SẮP DIỄN RA");
            statusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-background-color: #1e293b; -fx-padding: 4 8; -fx-border-radius: 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");
        } else {
            statusLabel.setText("● ĐÃ KẾT THÚC");
            statusLabel.setStyle("-fx-text-fill: #f87171; -fx-background-color: #450a0a; -fx-padding: 4 8; -fx-border-radius: 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");
        }

        currentBidLabel.setText(String.format("$%,.2f", auction.getCurrentHighestBid()));
        highestBidderLabel.setText(auction.getCurrentWinner() != null ? "Đấu thầu: @" + auction.getCurrentWinner().getUsername() : "Đấu thầu: Chưa có");
        bidCountLabel.setText("🔨 " + (auction.getBidHistory() != null ? auction.getBidHistory().size() : 0));
        if (viewerCountLabel != null) {
            viewerCountLabel.setText("👁 " + auction.getViewCount());
        }

        // Bật / tắt bảng đặt thầu theo vai trò người dùng và trạng thái phiên
        if (!(user instanceof Bidder)) {
            if (biddingVBox != null) {
                biddingVBox.setVisible(false);
                biddingVBox.setManaged(false);
            }
        } else {
            if (biddingVBox != null) {
                biddingVBox.setVisible(true);
                biddingVBox.setManaged(true);
            }
            // Thiết lập giá trị mặc định cho ô nhập thầu nếu chưa có
            if (bidAmountField.getText().trim().isEmpty() || isRunning) {
                double minAccepted = auction.getCurrentHighestBid() + auction.getMinBidStep();
                bidAmountField.setText(String.format("%.2f", minAccepted));
            }

            // Bật / tắt bảng đặt thầu theo trạng thái phiên
            bidAmountField.setDisable(!isRunning);
            confirmBidButton.setDisable(!isRunning);
            if (!isRunning) {
                confirmBidButton.setStyle("-fx-background-color: #262626; -fx-text-fill: #555555; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15; -fx-background-radius: 12;");
            } else {
                confirmBidButton.setStyle("-fx-background-color: #ffb95f; -fx-text-fill: #2a1700; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15; -fx-background-radius: 12; -fx-cursor: hand;");
            }
        }

        // Load ảnh sản phẩm
        String imageUrl = item.getImageUrl();
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("/")) {
                    File imageFile = new File("src/main/resources" + imageUrl);
                    if (imageFile.exists()) {
                        productImageView.setImage(new Image(imageFile.toURI().toString(), true));
                    } else {
                        productImageView.setImage(new Image("https://placehold.co/260x120/000000/FFFFFF/png?text=No+Image"));
                    }
                } else {
                    productImageView.setImage(new Image(imageUrl, true));
                }
            } else {
                productImageView.setImage(new Image("https://placehold.co/260x120/000000/FFFFFF/png?text=No+Image"));
            }
        } catch (Exception e) {
            productImageView.setImage(new Image("https://placehold.co/260x120/000000/FFFFFF/png?text=No+Image"));
        }

        renderBidHistory();
        renderPriceChart();
    }

    /**
     * Kết xuất danh sách lịch sử các lượt đặt giá thầu (Bid Transactions) lên giao diện.
     * Làm nổi bật lượt đặt giá thầu cao nhất hiện tại ở trên cùng.
     */
    private void renderBidHistory() {
        bidHistoryContainer.getChildren().clear();
        List<BidTransaction> history = auction.getBidHistory();
        if (history != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            for (int i = history.size() - 1; i >= 0; i--) {
                BidTransaction tx = history.get(i);

                HBox row = new HBox();
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10));

                boolean isHighest = (i == history.size() - 1);
                if (isHighest) {
                    row.setStyle("-fx-background-color: #ffb95f15; -fx-border-color: #ffb95f; -fx-border-width: 0 0 0 4; -fx-background-radius: 4;");
                } else {
                    row.setStyle("-fx-border-color: #45464d55; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
                }

                VBox infoCol = new VBox(2.0);
                Label nameLbl = new Label("@" + tx.getBidderName());
                nameLbl.setStyle("-fx-text-fill: #e4e2e4; -fx-font-weight: bold; -fx-font-size: 13px;");

                String timeStr = tx.getTimestamp() != null ? tx.getTimestamp().format(formatter) : "--:--:--";
                Label timeLbl = new Label(timeStr);
                timeLbl.setStyle("-fx-text-fill: #909097; -fx-font-size: 10px;");

                infoCol.getChildren().addAll(nameLbl, timeLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label amountLbl = new Label(String.format("$%,.2f", tx.getAmount()));
                if (isHighest) {
                    amountLbl.setStyle("-fx-text-fill: #ffb95f; -fx-font-weight: bold; -fx-font-size: 14px;");
                } else {
                    amountLbl.setStyle("-fx-text-fill: #909097; -fx-font-weight: bold; -fx-font-size: 14px;");
                }

                row.getChildren().addAll(infoCol, spacer, amountLbl);
                bidHistoryContainer.getChildren().add(row);
            }
        }
    }

    /**
     * Vẽ và cập nhật biểu đồ đường thể hiện lịch sử biến động giá thầu của phiên đấu giá.
     */
    private void renderPriceChart() {
        if (priceChart == null) return;
        priceChart.getData().clear();

        if (yAxis != null) {
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(100.0);
            double highestBid = auction.getCurrentHighestBid();
            double upper = Math.max(200.0, highestBid + 100.0);
            yAxis.setUpperBound(upper);

            // Tự động giãn khoảng chia (tick unit) khi khoảng giá quá rộng để đảm bảo thẩm mỹ trực quan
            double range = upper - 100.0;
            if (range > 1500.0) {
                yAxis.setTickUnit(Math.ceil(range / 10.0 / 100.0) * 100.0);
            } else {
                yAxis.setTickUnit(100.0);
            }
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        List<BidTransaction> history = auction.getBidHistory();

        if (history != null && !history.isEmpty()) {
            for (int i = 0; i < history.size(); i++) {
                series.getData().add(new XYChart.Data<>(i + 1, history.get(i).getAmount()));
            }
        } else {
            series.getData().add(new XYChart.Data<>(0, auction.getItem().getStartingPrice()));
        }
        priceChart.getData().add(series);
    }

    /**
     * Điều chỉnh (tăng) số tiền đặt giá thầu hiển thị trong ô nhập liệu thêm một lượng xác định.
     *
     * @param increment Lượng tiền muốn tăng thêm ($)
     */
    private void adjustBidAmount(double increment) {
        if (auction == null) return;
        double currentVal;
        try {
            currentVal = Double.parseDouble(bidAmountField.getText().trim());
        } catch (NumberFormatException e) {
            currentVal = auction.getCurrentHighestBid() + auction.getMinBidStep();
        }
        bidAmountField.setText(String.format("%.2f", currentVal + increment));
    }

    /**
     * Cộng thêm 100 $ vào ô nhập số tiền đặt giá thầu.
     */
    @FXML
    void addBid100() {
        adjustBidAmount(100.0);
    }

    /**
     * Cộng thêm 500 $ vào ô nhập số tiền đặt giá thầu.
     */
    @FXML
    void addBid500() {
        adjustBidAmount(500.0);
    }

    /**
     * Cộng thêm 1000 $ vào ô nhập số tiền đặt giá thầu.
     */
    @FXML
    void addBid1000() {
        adjustBidAmount(1000.0);
    }

    /**
     * Xử lý xác nhận đặt giá thầu khi người dùng nhấn nút Xác nhận.
     * Thực hiện kiểm tra tính hợp lệ của số tiền nhập, kiểm tra số dư ví và gửi yêu cầu đặt giá lên máy chủ.
     */
    @FXML
    void handleConfirmBid() {
        if (auction == null) return;
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            NotificationController.showError("Lỗi đặt giá", "Phiên đấu giá không ở trạng thái đang diễn ra (RUNNING).");
            return;
        }

        double minAccepted = auction.getCurrentHighestBid() + auction.getMinBidStep();

        try {
            double bidAmount = Double.parseDouble(bidAmountField.getText().trim());
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
                        double newBalance = (Double) res.getData();
                        bidder.setBalance(newBalance);
                        bidder.getProfile().addParticipatedAuction(auction.getId());
                    }
                    refreshAuctionDetails(false);
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

    /**
     * Hiển thị hộp thoại chi tiết đầy đủ thông tin về sản phẩm đang đấu giá (loại sản phẩm,
     * các thuộc tính đặc trưng như thương hiệu, họa sĩ, năm sản xuất,...).
     */
    @FXML
    private void handleViewItemDetails() {
        if (auction == null || auction.getItem() == null) return;
        Item item = auction.getItem();

        StringBuilder details = new StringBuilder();
        details.append("Tên sản phẩm: ").append(item.getName()).append("\n");
        details.append("Mô tả: ").append(item.getDescription()).append("\n");
        details.append("Trạng thái sản phẩm: ").append(item.getStatus()).append("\n");
        details.append("Trạng thái đấu giá: ").append(auction.getStatus()).append("\n");
        details.append("Giá cao nhất hiện tại: ").append(String.format("%,.2f", auction.getCurrentHighestBid())).append(" $\n");
        details.append("Bước giá tối thiểu: ").append(String.format("%,.2f", auction.getMinBidStep())).append(" $\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        details.append("Thời gian kết thúc: ").append(auction.getEndTime() != null ? auction.getEndTime().format(formatter) : "N/A").append("\n");

        if (item instanceof com.auction.model.item.Art art) {
            details.append("Họa sĩ: ").append(art.getArtist()).append("\n");
        } else if (item instanceof com.auction.model.item.Electronics electronics) {
            details.append("Thương hiệu: ").append(electronics.getBrand()).append("\n");
        } else if (item instanceof com.auction.model.item.Vehicle vehicle) {
            details.append("Năm sản xuất: ").append(vehicle.getYear()).append("\n");
        }

        NotificationController.showNotification("Chi tiết sản phẩm #" + item.getId(), details.toString());
    }

    /**
     * Dọn dẹp tài nguyên và các bộ lắng nghe khi đóng phòng đấu giá trực tiếp.
     *
     * @param decrementView true nếu muốn giảm lượt xem trên server
     */
    private void cleanupAndClose(boolean decrementView) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        openStages.remove(auction.getId());
        enteredAuctionIds.remove(auction.getId());
        ServerConnection.getInstance().removeNotificationHandler(realtimeListener);

        if (decrementView && auction != null) {
            Task<Response> closeTask = new Task<>() {
                @Override
                protected Response call() throws Exception {
                    GetAuctionDetailPayload payload = new GetAuctionDetailPayload(auction.getId(), false);
                    payload.decrementView = true;
                    return ServerConnection.getInstance().send(RequestType.GET_AUCTION_DETAIL, payload);
                }
            };

            Thread t = new Thread(closeTask);
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Quay lại danh sách phiên đấu giá (đóng cửa sổ, giữ nguyên lượt xem và observer).
     *
     * @param event Sự kiện hành động của JavaFX
     */
    @FXML
    private void handleBack(ActionEvent event) {
        cleanupAndClose(false);
        Stage stage = (Stage) backButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Thoát chính thức khỏi phòng đấu giá (bấm nút X). Giảm lượt xem và huỷ đăng ký observer trên server, sau đó đóng cửa sổ.
     *
     * @param event Sự kiện hành động của JavaFX
     */
    @FXML
    private void handleClose(ActionEvent event) {
        cleanupAndClose(true);
        Stage stage = (Stage) closeButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Cập nhật kiểu dáng và trạng thái hiển thị của nút Theo dõi (Watchlist) trong phòng chi tiết.
     *
     * @param btn Đối tượng nút Button cần cập nhật
     * @param isWatched Trạng thái theo dõi hiện tại
     */
    private void updateWatchlistButton(Button btn, boolean isWatched) {
        btn.setText(isWatched ? "★ ĐANG THEO DÕI" : "☆ THEO DÕI");
        if (isWatched) {
            btn.setStyle("-fx-background-color: #3b2f00; -fx-text-fill: #ffd700; -fx-border-color: #ffd700; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #ffd700; -fx-text-fill: #3b2f00; -fx-border-color: #ffd700; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3b2f00; -fx-text-fill: #ffd700; -fx-border-color: #ffd700; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;"));
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #909097; -fx-border-color: #45464d; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #ffd700; -fx-text-fill: #3b2f00; -fx-border-color: #ffd700; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #909097; -fx-border-color: #45464d; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;"));
        }
    }

    /**
     * Thực hiện thay đổi trạng thái theo dõi phiên đấu giá này (Thêm/Xóa khỏi Watchlist)
     * ngay trong màn hình chi tiết phòng đấu giá.
     */
    private void handleToggleWatchlistInDetail() {
        if (auction == null || !(user instanceof Bidder bidder)) return;
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
                if (watchlistButton != null) {
                    updateWatchlistButton(watchlistButton, !isWatched);
                }
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
}
