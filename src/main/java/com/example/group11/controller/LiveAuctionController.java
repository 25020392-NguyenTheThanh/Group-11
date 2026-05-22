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
import java.util.List;
import java.util.ResourceBundle;

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

    // Bộ sưu tập lưu trữ các ID phòng đấu giá đã được truy cập ở phiên làm việc này (tránh tăng view trùng lặp khi quay lại)
    private static final java.util.Set<Integer> enteredAuctionIds = new java.util.HashSet<>();

    // Bộ sưu tập lưu giữ các Stage của các phòng đấu giá đang mở song song
    private static final java.util.Map<Integer, Stage> openStages = new java.util.HashMap<>();

    public static Stage getOpenStage(int auctionId) {
        return openStages.get(auctionId);
    }

    public static void closeAllOpenStages() {
        Platform.runLater(() -> {
            for (Stage stage : new java.util.ArrayList<>(openStages.values())) {
                stage.close();
            }
            openStages.clear();
        });
    }

    /**
     * Dọn sạch danh sách phòng đấu giá đã truy cập (ví dụ khi người dùng đăng xuất hoặc đăng nhập).
     */
    public static void clearEnteredAuctions() {
        enteredAuctionIds.clear();
        closeAllOpenStages();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Chuẩn bị ban đầu
    }

    public void setAuctionAndUser(Auction auction, User user) {
        this.auction = auction;
        this.user = user;

        // Lưu thông tin Stage của cửa sổ mới mở vào openStages để quản lý song song
        Platform.runLater(() -> {
            if (backButton != null && backButton.getScene() != null) {
                Stage stage = (Stage) backButton.getScene().getWindow();
                if (stage != null) {
                    openStages.put(auction.getId(), stage);
                    stage.setOnCloseRequest(event -> {
                        openStages.remove(auction.getId());
                        if (countdownTimeline != null) {
                            countdownTimeline.stop();
                        }
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

    // Đồng bộ chi tiết phiên đấu giá từ Server (incrementView = true nếu muốn đếm thêm lượt xem)
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

    private void setupRealtimeNotifications() {
        ServerConnection.getInstance().setNotificationHandler(notification -> {
            Platform.runLater(() -> {
                System.out.println("LiveAuction nhận thông báo realtime: " + notification.getType() + " - " + notification.getData());
                if ("BID_UPDATE".equals(notification.getType()) 
                        || "AUCTION_ENDED".equals(notification.getType()) 
                        || "ITEM_STATUS_CHANGED".equals(notification.getType())) {
                    refreshAuctionDetails(false);
                }
            });
        });
    }

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

    private void renderPriceChart() {
        if (priceChart == null) return;
        priceChart.getData().clear();

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

    @FXML
    void addBid100() {
        adjustBidAmount(100.0);
    }

    @FXML
    void addBid500() {
        adjustBidAmount(500.0);
    }

    @FXML
    void addBid1000() {
        adjustBidAmount(1000.0);
    }

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
                        bidder.deduct(bidAmount);
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

    @FXML
    private void handleViewItemDetails() {
        if (auction == null || auction.getItem() == null) return;
        Item item = auction.getItem();

        StringBuilder details = new StringBuilder();
        details.append("Tên sản phẩm: ").append(item.getName()).append("\n");
        details.append("Mô tả: ").append(item.getDescription()).append("\n");
        details.append("Trạng thái sản phẩm: ").append(item.getStatus()).append("\n");
        details.append("Trạng thái đấu giá: ").append(auction.getStatus()).append("\n");
        details.append("Giá cao nhất hiện tại: ").append(String.format("%.2f", auction.getCurrentHighestBid())).append(" $\n");
        details.append("Bước giá tối thiểu: ").append(String.format("%.2f", auction.getMinBidStep())).append(" $\n");
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
     * Quay lại danh sách phiên đấu giá (đóng cửa sổ, giữ nguyên lượt xem và observer).
     */
    @FXML
    private void handleBack(ActionEvent event) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        Stage stage = (Stage) backButton.getScene().getWindow();
        if (stage != null) {
            openStages.remove(auction.getId());
            stage.close();
        }
    }

    /**
     * Thoát chính thức khỏi phòng đấu giá (bấm nút X). Giảm lượt xem và huỷ đăng ký observer trên server, sau đó đóng cửa sổ.
     */
    @FXML
    private void handleClose(ActionEvent event) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        if (auction != null) {
            enteredAuctionIds.remove(auction.getId());
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
        Stage stage = (Stage) closeButton.getScene().getWindow();
        if (stage != null) {
            openStages.remove(auction.getId());
            stage.close();
        }
    }
}
