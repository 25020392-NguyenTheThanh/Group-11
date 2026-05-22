package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.BidTransaction;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.BidUpdateData;
import com.auction.network.Notification;
import com.auction.network.PlaceBidPayload;
import com.auction.network.RequestType;
import com.auction.network.Response;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LiveAuctionController implements Initializable {
    @FXML private Label      productNameLabel;
    @FXML private Label      itemNameLabel;
    @FXML private Label      itemDescLabel;
    @FXML private Label      timerLabel;
    @FXML private Label      currentPriceLabel;
    @FXML private Label      currentWinnerLabel;
    @FXML private Label      totalBidsLabel;
    @FXML private Label      totalWatchersLabel;
    @FXML private VBox       bidHistoryContainer;
    @FXML private TextField  bidAmountField;
    @FXML private Button     confirmBidButton;
    @FXML private Button     backButton;
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    private Auction auction ;
    private User currentUser ;

    private ScheduledExecutorService timerScheduler ;
    private XYChart.Series<Number , Number> priceSeries ;
    private final AtomicInteger chartTick = new AtomicInteger(0);
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupChart();
    }
    // gọi từ BidderAuctionListController sau khi load FXML ──
    /**
     * Truyền auction + user vào controller.
     * PHẢI gọi method này sau FXMLLoader.load().
     */
    public void setAuction(Auction auction , User user){
        this.auction = auction ;
        this.currentUser = user ;

        renderStaticInfo(); // tên , mô tả sản phẩm
        refreshPriceUI(auction.getCurrentHighestBid(),
                        auction.getCurrentWinner() != null
                                ? auction.getCurrentWinner().getUsername() : "-",
                        auction.getBidHistory().size());
        renderBidHistory(auction.getBidHistory());
        addChartPoint(auction.getCurrentHighestBid());

        startCountdownTimer();
        setupRealtimeListener();

        // điền giá tối thiểu tiếp theo vào ô nhập
        if (bidAmountField != null){
            bidAmountField.setText(String.format("%.2f" , auction.getCurrentHighestBid() + auction.getMinBidStep()));
        }
    }

    // hiển thị thông tin tĩnh của sản phẩm
    private void renderStaticInfo(){
        if (auction.getItem() == null) return ;
        String name = auction.getItem().getName();
        String desc = auction.getItem().getDescription();
        if (productNameLabel != null) productNameLabel.setText(name);
        if (itemNameLabel != null) itemNameLabel.setText(name);
        if (itemDescLabel != null) itemDescLabel.setText(desc);
    }
    // cập nhật giá / người thắng / tổng số lượt đặt
    private void refreshPriceUI(double price , String winner , int totalBids){
        if (currentPriceLabel != null)
            currentPriceLabel.setText(String.format("$%,.0f" , price));
        if (currentWinnerLabel != null)
            currentWinnerLabel.setText("Đấu thầu : @" + winner);
        if (totalBidsLabel != null)
            totalBidsLabel.setText("" + totalBids);
    }
    // render lịch sử bid (mới nhất lên trên)
    private void renderBidHistory(List<BidTransaction> history){
        if (bidHistoryContainer == null) return ;
        bidHistoryContainer.getChildren().clear();

        int start = Math.max(0 , history.size() - 25); // tối đa 25 dòng
        for (int i = history.size()-1 ; i >= start ;i--){
            bidHistoryContainer.getChildren().add(
                    buildBidRow(history.get(i) , i == history.size() - 1)
            );
        }
    }

    private HBox buildBidRow(BidTransaction tx , boolean isTop){
        HBox row = new HBox(10);
        row.setStyle(isTop
            ? "-fx-background-color: #ffb95f15; -fx-border-color: #ffb95f; " +
              "-fx-border-width: 0 0 0 4; -fx-padding: 10; -fx-background-radius: 4;"
            : "-fx-border-color: #45464d55; -fx-border-width: 1; " +
              "-fx-padding: 10; -fx-border-radius: 4;");
        VBox info = new VBox(2);
        Label name = new Label("@" + tx.getBidderName());
        name.setStyle("-fx-text-fill: #e4e2e4; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label time = new Label(tx.getTimestamp() != null
                ? tx.getTimestamp().toLocalTime().toString().substring(0,8) :"--:--:--");
        time.setStyle("-fx-text-fill: #909097; -fx-font-size: 10px;");
        info.getChildren().addAll(name, time);

        Region spacer = new Region();
        HBox.setHgrow(spacer , Priority.ALWAYS);

        Label price = new Label(String.format("$%,.0f" , tx.getAmount()));
        price.setStyle(isTop
            ? "-fx-text-fill: #ffb95f; -fx-font-weight: bold; -fx-font-size: 14px;"
            : "-fx-text-fill: #909097; -fx-font-weight: bold; -fx-font-size: 14px;");
        row.getChildren().addAll(info , spacer , price);
        return row ;
    }
    // đồng hồ đếm ngược
    private void startCountdownTimer(){
        stopCountdownTimer();
        timerScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r , "LiveAuction-Timer-" + auction.getId());
            t.setDaemon(true);
            return t ;
        });
        timerScheduler.scheduleAtFixedRate(() -> {
            Duration left = Duration.between(LocalDateTime.now() , auction.getEndTime());
            Platform.runLater(() -> updateTimerLabel(left));
        } , 0 ,1 , TimeUnit.SECONDS);
    }

    private void updateTimerLabel(Duration left){
        if (timerLabel == null) return ;
        if (left.isNegative() || left.isZero()) {
            timerLabel.setText("00:00:00");
            timerLabel.setStyle(
                    "-fx-text-fill: #ff4444; -fx-font-size: 24px; -fx-font-weight: bold;");
            stopCountdownTimer();
            return ;
        }
        timerLabel.setText(String.format("%02d:%02d:%02d",
                left.toHours(), left.toMinutesPart(), left.toSecondsPart()));
        timerLabel.setStyle(left.toMinutes() < 5
                ? "-fx-text-fill: #ff6b35; -fx-font-size: 24px; -fx-font-weight: bold;"
                : "-fx-text-fill: #ffb95f; -fx-font-size: 24px; -fx-font-weight: bold;");
    }

    public void stopCountdownTimer() {
        if (timerScheduler != null && timerScheduler.isShutdown())
            timerScheduler.shutdown();
    }

    // Realtime listener
    /**
     * Ghi đè notification handler của ServerConnection.
     * Khi màn hình Live đang mở, TẤT CẢ notification đến đây xử lý.
     * Khi đóng màn hình, BidderAuctionListController sẽ set lại handler của nó.
     */

    private void setupRealtimeListener() {
        ServerConnection.getInstance().setNotificationHandler(notif ->
                Platform.runLater(() -> handleNotification(notif))
        );
    }

    private void handleNotification(Notification notif){
        String type = notif.getType();

        if ("BID_UPDATE".equals(type)){
            Object data = notif.getData();
            if (data instanceof BidUpdateData upd) {
                // Chỉ cập nhật nếu đúng phiên đang xem
                if (upd.auctionId != auction.getId()) return;

                // Cập nhật giá ngay lập tức — không cần gọi server
                refreshPriceUI(upd.newHighestBid, upd.winnerUsername, upd.totalBids);
                addChartPoint(upd.newHighestBid);

                // Điền giá tiếp theo vào ô nhập
                if (bidAmountField != null) {
                    bidAmountField.setText(
                            String.format("%.2f", upd.newHighestBid + auction.getMinBidStep())
                    );
                }

                // gọi lại server để lấy lịch sử bid mới nhất
                reloadBidHistoryFromServer();

            } else {
                // Fallback nếu server cũ vẫn gửi String: reload toàn bộ
                reloadBidHistoryFromServer();
            }

        } else if ("AUCTION_ENDED".equals(type)) {
            onAuctionEnded();
        }
    }

    private void reloadBidHistoryFromServer(){
        Task<Response> t = new Task<>(){
            @Override
            protected Response call(){
                return ServerConnection.getInstance().send(RequestType.GET_AUCTION_DETAIL , auction.getId());
            }
        };
        t.setOnSucceeded(e -> {
            Response res = t.getValue();
           if (res != null && res.isSuccess() && res.getData() instanceof Auction updated){
               // Cập nhật bid history trong object auction cục bộ
               auction.getBidHistory().clear();
               auction.getBidHistory().addAll(updated.getBidHistory());
               renderBidHistory(auction.getBidHistory());
           }
        });
        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    // khi server broadcast AUCTION_ENDED
    private void onAuctionEnded() {
        stopCountdownTimer();
        if (timerLabel != null) {
            timerLabel.setText("KẾT THÚC");
            timerLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 20px; -fx-font-weight: bold;");
        }
        if (confirmBidButton != null) {
            confirmBidButton.setDisable(true);
            confirmBidButton.setText("Phiên đã kết thúc");
        }
        if (bidAmountField != null) bidAmountField.setDisable(true);

        String winner = auction.getCurrentWinner() != null
                ? "Người thắng: @" + auction.getCurrentWinner().getUsername()
                  + " — $" + String.format("%,.0f", auction.getCurrentHighestBid())
                : "Không có người đặt giá.";
        NotificationController.showNotification("Phiên đấu giá kết thúc", winner);
    }
    // biểu đồ
    private void setupChart() {
        if (priceChart == null) return;
        priceSeries = new XYChart.Series<>();
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
        priceChart.setCreateSymbols(true);
        priceChart.setLegendVisible(false);
    }

    private void addChartPoint(double price) {
        if (priceSeries == null) return;
        int tick = chartTick.incrementAndGet();
        priceSeries.getData().add(new XYChart.Data<>(tick, price));

        // Giữ tối đa 30 điểm . mỗi điểm là (lần bid , giá bid)
        if (priceSeries.getData().size() > 30) {
            priceSeries.getData().remove(0);
        }
        if (chartXAxis != null) {
            chartXAxis.setLowerBound(Math.max(0, tick - 30));
            chartXAxis.setUpperBound(tick + 2);
        }
    }


    @FXML
    void handleConfirmBid() {
        if (auction == null || currentUser == null) return;

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            NotificationController.showError("Lỗi", "Phiên không còn hoạt động.");
            return;
        }

        // Parse số tiền
        String raw = bidAmountField != null ? bidAmountField.getText().trim() : "";
        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            NotificationController.showError("Lỗi nhập liệu", "Vui lòng nhập số hợp lệ.");
            return;
        }

        // Kiểm tra tối thiểu
        double minAccepted = auction.getCurrentHighestBid() + auction.getMinBidStep();
        if (amount < minAccepted) {
            NotificationController.showError("Lỗi đặt giá",
                    String.format("Phải đặt tối thiểu $%,.2f", minAccepted));
            return;
        }

        // Kiểm tra số dư
        if (currentUser instanceof Bidder b && b.getBalance() < amount) {
            NotificationController.showError("Số dư không đủ",
                    String.format("Số dư: $%,.2f — Cần: $%,.2f", b.getBalance(), amount));
            return;
        }

        // Disable nút tránh double-click
        if (confirmBidButton != null) confirmBidButton.setDisable(true);

        double finalAmount = amount;
        Task<Response> task = new Task<>() {
            @Override protected Response call() {
                PlaceBidPayload payload = new PlaceBidPayload();
                payload.auctionId = auction.getId();
                payload.amount    = finalAmount;
                return ServerConnection.getInstance().send(RequestType.PLACE_BID, payload);
            }
        };

        task.setOnSucceeded(e -> {
            if (confirmBidButton != null) confirmBidButton.setDisable(false);
            Response res = task.getValue();
            if (res != null && res.isSuccess()) {
                // Trừ tiền local ngay lập tức (không chờ server broadcast)
                if (currentUser instanceof Bidder b) b.deduct(finalAmount);
                // UI sẽ tự cập nhật khi BidUpdateData notification đến
            } else {
                String msg = res != null ? res.getMessage() : "Lỗi không xác định";
                NotificationController.showError("Lỗi đặt giá", msg);
            }
        });

        task.setOnFailed(e -> {
            if (confirmBidButton != null) confirmBidButton.setDisable(false);
            NotificationController.showError("Lỗi mạng", "Không thể kết nối tới server.");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    @FXML void addBid100()  { adjustBidField(100); }
    @FXML void addBid500()  { adjustBidField(500); }
    @FXML void addBid1000() { adjustBidField(1000); }

    // tự động cộng thêm tiền bid vào ô nhập khi user bấm các nút +100 / +500 / +1000
    private void adjustBidField(double delta) {
        if (bidAmountField == null) return;
        double base = auction != null
                ? auction.getCurrentHighestBid() + auction.getMinBidStep() : 0;
        double current;
        try {
            current = Double.parseDouble(bidAmountField.getText().trim());
        } catch (NumberFormatException ex) {
            current = base;
            // nhập lỗi -> lấy giá tối thiểu làm mặc định
        }
        bidAmountField.setText(String.format("%.2f", current + delta));
    }

    @FXML
    void handleBack() {
        stopCountdownTimer();
        // Đóng cửa sổ Live — BidderAuctionListController sẽ set lại handler trong setUser()
        if (backButton != null && backButton.getScene() != null) {
            ((javafx.stage.Stage) backButton.getScene().getWindow()).close();
        }
    }
    // Gọi khi Stage bị đóng — dọn dẹp timer
    public void cleanup() {
        stopCountdownTimer();
    }
}

