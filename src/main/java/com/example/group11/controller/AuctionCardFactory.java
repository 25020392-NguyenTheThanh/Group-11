package com.example.group11.controller;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.Bidder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Lớp nhà máy tạo giao diện thẻ phiên đấu giá (Auction Card Factory).
 * Cung cấp phương thức tĩnh để xây dựng giao diện hiển thị các phiên đấu giá dưới dạng thẻ (Card)
 * trực quan, sinh động dành cho người tham gia đấu giá (Bidder).
 */
public class AuctionCardFactory {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    /**
     * Tạo một giao diện thẻ phiên đấu giá (Auction Card) hiển thị thông tin chi tiết về phiên đấu giá,
     * sản phẩm, giá hiện tại, thời gian bắt đầu/kết thúc và các nút tương tác (Đặt giá, Chi tiết, Theo dõi).
     *
     * @param auction Đối tượng phiên đấu giá chứa dữ liệu cần hiển thị
     * @param onBidAction Hành động xử lý khi người dùng nhấn nút "ĐẶT GIÁ"
     * @param onViewDetailsAction Hành động xử lý khi người dùng nhấn nút "CHI TIẾT"
     * @param isWatched Trạng thái cho biết phiên đấu giá này có đang được người dùng theo dõi hay không
     * @param onWatchlistToggleAction Hành động xử lý khi người dùng thay đổi trạng thái theo dõi phiên đấu giá
     * @param user Đối tượng người dùng hiện tại đang đăng nhập hệ thống
     * @return VBox đối tượng giao diện JavaFX chứa thẻ phiên đấu giá hoàn chỉnh
     */
    public static VBox createAuctionCard(Auction auction, Consumer<Auction> onBidAction, Consumer<Auction> onViewDetailsAction, boolean isWatched, Consumer<Auction> onWatchlistToggleAction, User user) {
        Item item = auction.getItem();
        if (item == null) return new VBox();

        // 1. Container chính của Card (Tương đương VBox fx:id="mainCardContainer")
        VBox cardContainer = new VBox();
        cardContainer.setMaxWidth(260.0);
        cardContainer.setPrefWidth(260.0);
        cardContainer.setStyle(
                "-fx-background-color: #1A1A1A; " +
                        "-fx-border-color: #262626; " +
                        "-fx-border-width: 1; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15;"
        );

        // --- HEADER BAR (ID & Trạng thái) ---
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPrefHeight(35.0);
        headerBar.setSpacing(10.0);
        headerBar.setPadding(new Insets(0, 15, 0, 15));
        headerBar.setStyle("-fx-background-color: #0A0A0A; -fx-border-color: transparent transparent #262626 transparent; -fx-background-radius: 15 15 0 0; -fx-border-radius: 15 15 0 0;");

        Label auctionIdLabel = new Label("ID: #" + auction.getId());
        auctionIdLabel.setStyle("-fx-text-fill: #d0c6ab; -fx-font-weight: bold;");
        auctionIdLabel.setFont(Font.font("System", 9.0));

        // Khoảng trống đẩy trạng thái về bên phải
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        // Container trạng thái (Dot + Text)
        HBox statusContainer = new HBox();
        statusContainer.setAlignment(Pos.CENTER);
        statusContainer.setSpacing(5.0);

        Circle statusDot = new Circle(3.0);
        statusDot.setId("statusDot");
        statusDot.setStroke(Color.TRANSPARENT);
        Label statusLabel = new Label();
        statusLabel.setId("statusText");
        statusLabel.setFont(Font.font("System", 10.0));

        // Cấu hình màu sắc theo trạng thái
        String statusColor = "#94A3B8";
        String statusText = auction.getStatus().toString();
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            statusColor = "#00e475";
        } else if (auction.getStatus() == AuctionStatus.OPEN) {
            statusColor = "#3B82F6";
        } else if (auction.getStatus() == AuctionStatus.FINISHED) {
            statusColor = "#F59E0B";
        } else if (auction.getStatus() == AuctionStatus.PAID) {
            statusColor = "#10B981";
        } else if (auction.getStatus() == AuctionStatus.CANCELED) {
            statusColor = "#EF4444";
        }
        statusDot.setFill(Color.valueOf(statusColor));
        statusLabel.setText(statusText);
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold;");
        statusContainer.getChildren().addAll(statusDot, statusLabel);

        // Nhãn đếm ngược thời gian còn lại (timerLabel)
        Label timerLabel = new Label();
        timerLabel.setId("timer");
        timerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        timerLabel.setFont(Font.font("System", 12.0));
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            Duration duration = Duration.between(java.time.LocalDateTime.now(), auction.getEndTime());
            if (!duration.isNegative()) {
                long hours = duration.toHours();
                long minutes = duration.toMinutesPart();
                long seconds = duration.toSecondsPart();
                timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            } else {
                timerLabel.setText("00:00:00");
            }
        } else if (auction.getStatus() == AuctionStatus.OPEN) {
            timerLabel.setText("WAITING");
        } else if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
            timerLabel.setText("ENDED");
        } else if (auction.getStatus() == AuctionStatus.CANCELED) {
            timerLabel.setText("CANCELED");
        }

        headerBar.getChildren().addAll(auctionIdLabel, spacerHeader, statusContainer, timerLabel);

        // --- HÌNH ẢNH SẢN PHẨM ---
        String imageUrl = item.getImageUrl();
        StackPane imgStack = new StackPane();
        imgStack.setPrefHeight(120.0);
        imgStack.setStyle("-fx-background-color: #000000;");

        ImageView imgView = new ImageView();
        imgView.setFitHeight(120.0);
        imgView.setFitWidth(260.0);
        imgView.setPreserveRatio(true);
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("/")) {
                    // Giải pháp tối ưu: Đọc trực tiếp từ ổ đĩa (File System) dựa trên thư mục mã nguồn src
                    File imageFile = new java.io.File("src/main/resources" + imageUrl);
                    if (imageFile.exists()) {
                        imgView.setImage(new Image(imageFile.toURI().toString(), true));
                    } else {
                        System.err.println("Không tìm thấy file ảnh thực tế tại: " + imageFile.getAbsolutePath());
                        imgView.setImage(new Image("https://placehold.co/260x120/000000/FFFFFF/png?text=No+Image"));
                    }
                } else {
                    imgView.setImage(new Image(imageUrl, true));
                }
            } else {
                imgView.setImage(new Image("https://placehold.co/260x120/000000/FFFFFF/png?text=No+Image"));
            }
        } catch (Exception e) {
            System.err.println("Lỗi bất ngờ khi load ảnh sản phẩm: " + e.getMessage());
            imgView.setImage(new Image("https://placehold.co/260x120/000000/FFFFFF/png?text=No+Image"));
        }
        imgStack.getChildren().add(imgView);

        // Outcomes badge for ended auctions (FINISHED, PAID, CANCELED)
        if (auction.getStatus() == AuctionStatus.FINISHED 
                || auction.getStatus() == AuctionStatus.PAID 
                || auction.getStatus() == AuctionStatus.CANCELED) {
            Label resultBadge = new Label();
            resultBadge.setPadding(new Insets(4, 8, 4, 8));
            resultBadge.setFont(Font.font("System", FontWeight.BOLD, 10.0));
            
            if (auction.getStatus() == AuctionStatus.CANCELED) {
                resultBadge.setText("ĐÃ HỦY");
                resultBadge.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 0 0 0 10; -fx-font-weight: bold;");
            } else {
                boolean isWinner = false;
                if (user instanceof Bidder bidder) {
                    isWinner = (auction.getCurrentWinner() != null && auction.getCurrentWinner().getId() == bidder.getId()) 
                            || bidder.getProfile().getWonAuctions().contains(auction.getId());
                }
                if (isWinner) {
                    resultBadge.setText("🏆 BẠN ĐÃ THẮNG");
                    resultBadge.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 0 0 0 10; -fx-font-weight: bold;");
                } else {
                    resultBadge.setText("THẤT BẠI");
                    resultBadge.setStyle("-fx-background-color: #4B5563; -fx-text-fill: white; -fx-background-radius: 0 0 0 10; -fx-font-weight: bold;");
                }
            }
            StackPane.setAlignment(resultBadge, Pos.TOP_RIGHT);
            imgStack.getChildren().add(resultBadge);
        }

        // --- BODY CONTAINER (Thông tin sản phẩm) ---
        VBox bodyContainer = new VBox();
        bodyContainer.setSpacing(8.0);
        bodyContainer.setPadding(new Insets(8, 12, 8, 12));

        // Tên và mô tả sản phẩm
        VBox nameDescContainer = new VBox(2.0);

        Label productNameLabel = new Label(item.getName());
        productNameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        productNameLabel.setFont(Font.font("System", 14.0));
        productNameLabel.setWrapText(true);

        Label descriptionLabel = new Label(item.getDescription());
        descriptionLabel.setStyle("-fx-text-fill: #d0c6ab;");
        descriptionLabel.setFont(Font.font("System", 11.0));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMinHeight(18.0); // Giữ khoảng trống đều nhau giữa các card

        nameDescContainer.getChildren().addAll(productNameLabel, descriptionLabel);

        // --- KHU VỰC HIỂN THỊ GIÁ (Pricing Section) ---
        VBox priceContainer = new VBox(2.0);
        
        String borderCol = "#ffd700";
        if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
            borderCol = "#10B981";
        } else if (auction.getStatus() == AuctionStatus.CANCELED) {
            borderCol = "#EF4444";
        }
        priceContainer.setStyle("-fx-background-color: #0A0A0A; -fx-padding: 4 10; -fx-border-color: transparent transparent transparent " + borderCol + "; -fx-border-width: 0 0 0 3;");

        VBox startPriceBox = new VBox();
        Label lblStartTitle = new Label("GIÁ KHỞI ĐIỂM");
        lblStartTitle.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-weight: bold;");
        lblStartTitle.setFont(Font.font("System", 8.0));

        Label startingPriceLabel = new Label(String.format("%,.2f $", item.getStartingPrice()));
        startingPriceLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8);");
        startingPriceLabel.setFont(Font.font("System", 11.0));
        startPriceBox.getChildren().addAll(lblStartTitle, startingPriceLabel);

        VBox currentPriceBox = new VBox(0.0);
        Label lblCurrentTitle = new Label();
        lblCurrentTitle.setFont(Font.font("System", 9.0));
        
        if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
            lblCurrentTitle.setText("GIÁ CHUNG CUỘC");
            lblCurrentTitle.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        } else if (auction.getStatus() == AuctionStatus.CANCELED) {
            lblCurrentTitle.setText("GIÁ KHI HỦY");
            lblCurrentTitle.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
        } else {
            lblCurrentTitle.setText("GIÁ HIỆN TẠI CAO NHẤT");
            lblCurrentTitle.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold;");
        }

        Label currentPriceLabel = new Label(String.format("%,.2f $", auction.getCurrentHighestBid()));
        currentPriceLabel.setId("currentPrice");
        if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
            currentPriceLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        } else if (auction.getStatus() == AuctionStatus.CANCELED) {
            currentPriceLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
        } else {
            currentPriceLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold;");
        }
        currentPriceLabel.setFont(Font.font("System", 18.0));
        currentPriceBox.getChildren().addAll(lblCurrentTitle, currentPriceLabel);

        priceContainer.getChildren().addAll(startPriceBox, currentPriceBox);

        // Add winner name if ended
        if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
            String winnerName = (auction.getCurrentWinner() != null) ? auction.getCurrentWinner().getUsername() : "Không có";
            boolean isWinner = false;
            if (user instanceof Bidder bidder) {
                isWinner = (auction.getCurrentWinner() != null && auction.getCurrentWinner().getId() == bidder.getId()) 
                        || bidder.getProfile().getWonAuctions().contains(auction.getId());
            }
            
            Label winnerLabel = new Label(isWinner ? "BẠN" : winnerName);
            winnerLabel.setFont(Font.font("System", 10.0));
            winnerLabel.setStyle(isWinner ? "-fx-text-fill: #10B981; -fx-font-weight: bold;" : "-fx-text-fill: #94A3B8;");
            
            VBox winnerBox = new VBox(0.0);
            Label lblWinnerTitle = new Label("NGƯỜI THẮNG CUỘC");
            lblWinnerTitle.setFont(Font.font("System", 8.0));
            lblWinnerTitle.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-weight: bold;");
            
            winnerBox.getChildren().addAll(lblWinnerTitle, winnerLabel);
            priceContainer.getChildren().add(winnerBox);
        }

        // --- KHU VỰC THỜI GIAN (Dates Section) ---
        HBox timeContainer = new HBox();
        timeContainer.setSpacing(0.0);
        timeContainer.setStyle("-fx-border-color: #262626 transparent #262626 transparent; -fx-border-width: 1 0 1 0; -fx-padding: 4 0;");

        VBox startBox = new VBox(2.0);
        HBox.setHgrow(startBox, Priority.ALWAYS);
        Label lblStart = new Label("BẮT ĐẦU");
        lblStart.setStyle("-fx-text-fill: #d0c6ab;");
        lblStart.setFont(Font.font("System", 8.0));

        String startTimeStr = auction.getStartTime() != null ? auction.getStartTime().format(DATE_FORMATTER) : "--:--";
        Label startTimeLabel = new Label(startTimeStr);
        startTimeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        startTimeLabel.setFont(Font.font("System", 11.0));
        startBox.getChildren().addAll(lblStart, startTimeLabel);

        VBox endBox = new VBox(2.0);
        endBox.setStyle("-fx-border-color: transparent transparent transparent #262626; -fx-border-width: 0 0 0 1; -fx-padding: 0 0 0 15;");
        HBox.setHgrow(endBox, Priority.ALWAYS);
        Label lblEnd = new Label("KẾT THÚC");
        lblEnd.setStyle("-fx-text-fill: #d0c6ab;");
        lblEnd.setFont(Font.font("System", 8.0));

        String endTimeStr = auction.getEndTime() != null ? auction.getEndTime().format(DATE_FORMATTER) : "--:--";
        Label endTimeLabel = new Label(endTimeStr);
        endTimeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        endTimeLabel.setFont(Font.font("System", 11.0));
        endBox.getChildren().addAll(lblEnd, endTimeLabel);

        timeContainer.getChildren().addAll(startBox, endBox);


        // --- HÀNH ĐỘNG BUTTONS ---
        VBox actionBox = new VBox(4.0);

        // Nút ĐẶT GIÁ
        Button bidButton = new Button("ĐẶT GIÁ");
        bidButton.setId("bidButton");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bidButton, Priority.ALWAYS);

        boolean isWinner = false;
        if (user instanceof Bidder bidder) {
            isWinner = (auction.getCurrentWinner() != null && auction.getCurrentWinner().getId() == bidder.getId()) 
                    || bidder.getProfile().getWonAuctions().contains(auction.getId());
        }

        // Thiết lập trạng thái và màu sắc kích hoạt nút ĐẶT GIÁ
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            bidButton.setText("ĐẶT GIÁ");
            bidButton.setDisable(false);
            bidButton.setStyle("-fx-background-color: #ffd700; -fx-text-fill: #3a3000; -fx-font-weight: bold; -fx-padding: 6; -fx-background-radius: 2; -fx-cursor: hand;");
        } else if (auction.getStatus() == AuctionStatus.FINISHED && isWinner) {
            bidButton.setText("THANH TOÁN");
            bidButton.setDisable(false);
            bidButton.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6; -fx-background-radius: 2; -fx-cursor: hand;");
        } else {
            bidButton.setDisable(true);
            if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
                bidButton.setText("ĐÃ KẾT THÚC");
            } else if (auction.getStatus() == AuctionStatus.CANCELED) {
                bidButton.setText("ĐÃ HỦY");
            } else {
                bidButton.setText("CHỜ BẮT ĐẦU");
            }
            bidButton.setStyle("-fx-background-color: #262626; -fx-text-fill: #777777; -fx-font-weight: bold; -fx-padding: 6; -fx-background-radius: 2;");
        }

        // Nút CHI TIẾT
        Button detailsButton = new Button("CHI TIẾT");
        detailsButton.setMaxWidth(Double.MAX_VALUE);
        detailsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-radius: 2; -fx-background-radius: 2; -fx-padding: 6; -fx-cursor: hand;");

        // Gắn sự kiện (Callbacks)
        bidButton.setOnAction(e -> {
            if (onBidAction != null) onBidAction.accept(auction);
        });

        detailsButton.setOnAction(e -> {
            if (onViewDetailsAction != null) onViewDetailsAction.accept(auction);
        });

        // Hàng chứa nút Theo dõi và Đặt giá
        HBox topRow = new HBox(4.0);
        topRow.setMaxWidth(Double.MAX_VALUE);

        // Nút Watchlist (Theo dõi ở bên trái)
        if (onWatchlistToggleAction != null) {
            Button watchlistButton = new Button(isWatched ? "★ BỎ THEO DÕI" : "☆ THEO DÕI");
            watchlistButton.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(watchlistButton, Priority.ALWAYS);
            if (isWatched) {
                // Đã chọn: Màu xanh dương
                watchlistButton.setStyle("-fx-background-color: #0b1e36; -fx-text-fill: #38BDF8; -fx-border-color: #38BDF8; -fx-border-radius: 2; -fx-background-radius: 2; -fx-font-weight: bold; -fx-padding: 6; -fx-cursor: hand;");
                watchlistButton.setOnMouseEntered(e -> watchlistButton.setStyle("-fx-background-color: #38BDF8; -fx-text-fill: #0b1e36; -fx-border-color: #38BDF8; -fx-border-radius: 2; -fx-background-radius: 2; -fx-font-weight: bold; -fx-padding: 6; -fx-cursor: hand;"));
                watchlistButton.setOnMouseExited(e -> watchlistButton.setStyle("-fx-background-color: #0b1e36; -fx-text-fill: #38BDF8; -fx-border-color: #38BDF8; -fx-border-radius: 2; -fx-background-radius: 2; -fx-font-weight: bold; -fx-padding: 6; -fx-cursor: hand;"));
            } else {
                // Bình thường: Màu xám
                watchlistButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-border-color: #94A3B8; -fx-border-radius: 2; -fx-background-radius: 2; -fx-padding: 6; -fx-cursor: hand;");
                // Di chuột: Màu xám nhạt
                watchlistButton.setOnMouseEntered(e -> watchlistButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #E2E8F0; -fx-border-color: #E2E8F0; -fx-border-radius: 2; -fx-background-radius: 2; -fx-padding: 6; -fx-cursor: hand;"));
                watchlistButton.setOnMouseExited(e -> watchlistButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-border-color: #94A3B8; -fx-border-radius: 2; -fx-background-radius: 2; -fx-padding: 6; -fx-cursor: hand;"));
            }
            watchlistButton.setOnAction(e -> onWatchlistToggleAction.accept(auction));
            topRow.getChildren().add(watchlistButton);
        }

        // Nút Đặt giá ở bên phải
        topRow.getChildren().add(bidButton);

        actionBox.getChildren().addAll(topRow, detailsButton);

        // Gom tất cả vào Body, rồi gom Body + Header vào Card chính
        bodyContainer.getChildren().addAll(nameDescContainer, priceContainer, timeContainer, actionBox);
        cardContainer.getChildren().addAll(headerBar, imgStack, bodyContainer);

        return cardContainer;
    }

    /**
     * Cập nhật thông tin động của card (giá, trạng thái, timer) mà không rebuild lại toàn bộ UI.
     * Gọi method này thay vì tạo card mới khi nhận được giá cập nhật.
     */
    public static void updateCard(VBox card, Auction auction) {
        Label currentPriceLabel = (Label) card.lookup("#currentPrice");
        Label timerLabel        = (Label) card.lookup("#timer");
        Label statusLabel       = (Label) card.lookup("#statusText");
        Circle statusDot        = (Circle) card.lookup("#statusDot");
        Button bidButton        = (Button) card.lookup("#bidButton");

        if (currentPriceLabel == null) return; // Card chưa gán ID, bỏ qua

        // 1. Cập nhật giá
        currentPriceLabel.setText(String.format("%,.2f $", auction.getCurrentHighestBid()));

        // 2. Cập nhật timer
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            java.time.Duration d = java.time.Duration.between(java.time.LocalDateTime.now(), auction.getEndTime());
            if (!d.isNegative()) {
                timerLabel.setText(String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart()));
            } else {
                timerLabel.setText("00:00:00");
            }
        } else if (auction.getStatus() == AuctionStatus.OPEN) {
            timerLabel.setText("WAITING");
        } else {
            timerLabel.setText("ENDED");
        }

        // 3. Cập nhật trạng thái (nếu thay đổi)
        String color = getStatusColor(auction.getStatus());
        statusLabel.setText(auction.getStatus().toString());
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        statusDot.setFill(Color.valueOf(color));

        // 4. Cập nhật nút ĐẶT GIÁ
        if (bidButton != null) {
            if (auction.getStatus() == AuctionStatus.RUNNING) {
                bidButton.setText("ĐẶT GIÁ");
                bidButton.setDisable(false);
                bidButton.setStyle("-fx-background-color: #ffd700; -fx-text-fill: #3a3000; -fx-font-weight: bold; -fx-padding: 6; -fx-background-radius: 2; -fx-cursor: hand;");
            } else {
                bidButton.setDisable(true);
                if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID) {
                    bidButton.setText("ĐÃ KẾT THÚC");
                } else if (auction.getStatus() == AuctionStatus.CANCELED) {
                    bidButton.setText("ĐÃ HỦY");
                } else {
                    bidButton.setText("CHỜ BẮT ĐẦU");
                }
                bidButton.setStyle("-fx-background-color: #262626; -fx-text-fill: #777777; -fx-font-weight: bold; -fx-padding: 6; -fx-background-radius: 2;");
            }
        }
    }

    private static String getStatusColor(AuctionStatus status) {
        return switch (status) {
            case RUNNING  -> "#00e475";
            case OPEN     -> "#3B82F6";
            case FINISHED -> "#F59E0B";
            case PAID     -> "#10B981";
            case CANCELED -> "#EF4444";
            default       -> "#94A3B8";
        };
    }
}







