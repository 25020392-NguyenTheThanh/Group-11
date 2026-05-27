package com.example.group11.controller;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import com.auction.model.auction.Auction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Lớp nhà máy tạo giao diện thẻ sản phẩm (Product Card Factory).
 * Cung cấp phương thức để xây dựng giao diện hiển thị thông tin sản phẩm trực quan dưới dạng thẻ (Card)
 * dành riêng cho người bán (Seller).
 */
public class ProductCardFactory {
    /**
     * Tạo một giao diện thẻ sản phẩm (Product Card) hiển thị thông tin của sản phẩm
     * cùng các nút điều khiển Chi tiết, Sửa và Xóa cho người bán.
     *
     * @param item Đối tượng sản phẩm (Art, Electronics, hoặc Vehicle) chứa thông tin cần hiển thị
     * @param onDetailsClick Hành động (BiConsumer) được thực thi khi nhấn nút "CHI TIẾT"
     * @param onEditClick Hành động (BiConsumer) được thực thi khi nhấn nút "SỬA"
     * @param onActionClick Hành động (BiConsumer) được thực thi khi xác nhận xóa sản phẩm
     * @return VBox đối tượng giao diện JavaFX chứa thẻ sản phẩm hoàn chỉnh
     */
    public static VBox createProductCard(Item item, Auction auction, BiConsumer<Item, VBox> onDetailsClick, BiConsumer<Item, VBox> onEditClick, BiConsumer<Item, VBox> onActionClick) {
        // 1. Lấy thông tin chung từ DB
        String id = String.valueOf(item.getId());
        String name = item.getName();
        String desc = item.getDescription();
        String startPrice = String.valueOf(item.getStartingPrice());
        String imageUrl = item.getImageUrl();

        // 2. TỰ ĐỘNG PHÂN LOẠI THUỘC TÍNH DỰA TRÊN CLASS THỰC TẾ TRONG DB
        String attributeKey = "";
        String attributeValue = "";

        if (item instanceof Art art) {
            attributeKey = "Tác giả";
            attributeValue = art.getArtist();
        } else if (item instanceof Electronics electronics) {
            attributeKey = "Thương hiệu";
            attributeValue = electronics.getBrand();
        } else if (item instanceof Vehicle vehicle) {
            attributeKey = "Năm SX";
            attributeValue = String.valueOf(vehicle.getYear());
        }
        // Container chính (mainCardContainer trong FXML)
        VBox card = new VBox();
        card.setId("product-card-" + item.getId());
        card.setPrefWidth(260.0);
        card.setMaxWidth(260.0);
        card.setStyle("-fx-background-color: #1A1A1A; -fx-border-color: #262626; -fx-border-width: 1; -fx-background-radius: 15; -fx-border-radius: 15;");

        // Header: ID, Status
        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setPrefHeight(35.0);
        header.setSpacing(8.0);
        header.setPadding(new Insets(0, 10, 0, 10));
        header.setStyle("-fx-background-color: #0A0A0A; -fx-border-color: transparent transparent #262626 transparent; -fx-background-radius: 15 15 0 0; -fx-border-radius: 15 15 0 0;");

        Label lblId = new Label("ID: " + id);
        lblId.setStyle("-fx-text-fill: #d0c6ab; -fx-font-weight: bold;");
        lblId.setFont(new Font(9.0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox statusBox = new HBox(4);
        statusBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        String statusText = "AVAILABLE";
        String statusColor = "#00e475"; // Green
        
        if (item.getStatus() != null) {
            switch (item.getStatus()) {
                case IN_AUCTION -> {
                    statusText = "IN AUCTION";
                    statusColor = "#ff5722"; // Red-orange
                }
                case SOLD -> {
                    statusText = "SOLD";
                    statusColor = "#2196f3"; // Blue
                }
                case UNSOLD -> {
                    statusText = "UNSOLD";
                    statusColor = "#757575"; // Gray
                }
                default -> {
                    statusText = "AVAILABLE";
                    statusColor = "#00e475"; // Green
                }
            }
        }
        
        Circle dot = new Circle(3.0, javafx.scene.paint.Color.web(statusColor));
        Label lblStatus = new Label(statusText);
        lblStatus.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold;");
        lblStatus.setFont(new Font(9.0));
        statusBox.getChildren().addAll(dot, lblStatus);

        header.getChildren().addAll(lblId, spacer, statusBox);

        // Hình ảnh sản phẩm
        StackPane imgStack = new StackPane();
        imgStack.setPrefHeight(135.0);
        imgStack.setStyle("-fx-background-color: #000000;");

        ImageView imgView = new ImageView();
        imgView.setFitHeight(135.0);
        imgView.setFitWidth(260.0);
        imgView.setPreserveRatio(true);
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("/")) {
                    // Giải pháp tối ưu: Đọc trực tiếp từ ổ đĩa (File System) dựa trên thư mục mã nguồn src
                    // Điều này giúp ảnh vừa đăng ký lập tức hiển thị mà không cần build lại dự án
                    java.io.File imageFile = new java.io.File("src/main/resources" + imageUrl);

                    if (imageFile.exists()) {
                        // Chuyển đổi file thành định dạng URL hệ thống tệp (file:/...)
                        imgView.setImage(new Image(imageFile.toURI().toString(), true));
                    } else {
                        System.err.println("Không tìm thấy file ảnh thực tế tại: " + imageFile.getAbsolutePath());
                        imgView.setImage(new Image("https://placehold.co/260x135/000000/FFFFFF/png?text=No+Image"));
                    }
                } else {
                    // Load ảnh từ URL mạng thông thường nếu có
                    imgView.setImage(new Image(imageUrl, true));
                }
            } else {
                imgView.setImage(new Image("https://placehold.co/260x135/000000/FFFFFF/png?text=No+Image"));
            }
        } catch (Exception e) {
            System.err.println("Lỗi bất ngờ khi load ảnh sản phẩm: " + e.getMessage());
            imgView.setImage(new Image("https://placehold.co/260x135/000000/FFFFFF/png?text=No+Image"));
        }
        imgStack.getChildren().add(imgView);

        // Nội dung thông tin (VBox chính bên dưới ảnh)
        VBox content = new VBox(10.0);
        content.setPadding(new Insets(12, 12, 12, 12));

        // Tên và mô tả
        VBox nameDesc = new VBox(3.0);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        lblName.setFont(new Font(14.0));
        lblName.setWrapText(true);

        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-text-fill: #d0c6ab;");
        lblDesc.setFont(new Font(11.0));
        lblDesc.setWrapText(true);
        nameDesc.getChildren().addAll(lblName, lblDesc);

        // Thuộc tính đặc trưng (HBox thương hiệu)
        HBox attrBox = new HBox(6.0);
        attrBox.setAlignment(Pos.CENTER_LEFT);
        attrBox.setStyle("-fx-background-color: #222222; -fx-padding: 6 10; -fx-background-radius: 5;");

        Label lblAttrKey = new Label(attributeKey.toUpperCase() + ":");
        lblAttrKey.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold;");
        lblAttrKey.setFont(new Font(11.0));

        Label lblAttrVal = new Label(attributeValue);
        lblAttrVal.setStyle("-fx-text-fill: white;");
        lblAttrVal.setFont(new Font(11.0));
        attrBox.getChildren().addAll(lblAttrKey, lblAttrVal);

        // Khu vực giá
        VBox priceContainer = new VBox(4.0);
        priceContainer.setStyle("-fx-background-color: #0A0A0A; -fx-padding: 8 12; -fx-border-color: transparent transparent transparent #ffd700; -fx-border-width: 0 0 0 3;");

        VBox startPriceBox = new VBox();
        Label tStart = new Label("GIÁ KHỞI ĐIỂM");
        tStart.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-weight: bold;");
        tStart.setFont(new Font(8.0));
        Label vStart = new Label(String.format("%,.2f $", item.getStartingPrice()));
        vStart.setStyle("-fx-text-fill: rgba(255,255,255,0.8);");
        vStart.setFont(new Font(11.0));
        startPriceBox.getChildren().addAll(tStart, vStart);

        priceContainer.getChildren().addAll(startPriceBox);

        if (auction != null) {
            VBox currentPriceBox = new VBox();
            Label lblCurrentTitle = new Label("GIÁ HIỆN TẠI CAO NHẤT");
            lblCurrentTitle.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold;");
            lblCurrentTitle.setFont(new Font(8.0));
            Label currentPriceLabel = new Label(String.format("%,.2f $", auction.getCurrentHighestBid()));
            currentPriceLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold;");
            currentPriceLabel.setFont(new Font(14.0));
            currentPriceBox.getChildren().addAll(lblCurrentTitle, currentPriceLabel);
            priceContainer.getChildren().add(currentPriceBox);
        }
        // Tạo nút CHI TIẾT dùng chung cho cả 2 vai trò
        HBox actions = new HBox(8.0);
        actions.setAlignment(Pos.CENTER);
        Button btnDetails = new Button("CHI TIẾT");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDetails, javafx.scene.layout.Priority.ALWAYS);
        btnDetails.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8; -fx-cursor: hand; -fx-background-radius: 4;");
        btnDetails.setOnAction(e -> {
            if (onDetailsClick != null) {
                onDetailsClick.accept(item, card);
            }
        });
        actions.getChildren().add(btnDetails);

        // Giao diện phía Seller: Hiện nút quản lý (SỬA, XÓA)
        Button btnEdit = new Button("SỬA");
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnEdit, javafx.scene.layout.Priority.ALWAYS);
        
        boolean canEdit = false;
        if (item.getStatus() == com.auction.model.item.ItemStatus.AVAILABLE) {
            canEdit = true;
        } else if (item.getStatus() == com.auction.model.item.ItemStatus.IN_AUCTION) {
            if (auction != null && auction.getStatus() == com.auction.model.auction.AuctionStatus.OPEN) {
                canEdit = true;
            }
        }
        
        if (!canEdit) {
            btnEdit.setDisable(true);
            btnEdit.setStyle("-fx-background-color: #475569; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 8; -fx-background-radius: 4; -fx-opacity: 0.5;");
        } else {
            btnEdit.setStyle("-fx-background-color: #f57c00; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8; -fx-cursor: hand; -fx-background-radius: 4;");
        }
        btnEdit.setOnAction(e -> {
            if (onEditClick != null) {
                onEditClick.accept(item, card);
            }
        });

        Button btnDelete = new Button("XÓA");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDelete, javafx.scene.layout.Priority.ALWAYS);
        if (item.getStatus() == com.auction.model.item.ItemStatus.IN_AUCTION) {
            btnDelete.setDisable(true);
            btnDelete.setStyle("-fx-background-color: #475569; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 8; -fx-background-radius: 4; -fx-opacity: 0.5;");
        } else {
            btnDelete.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8; -fx-cursor: hand; -fx-background-radius: 4;");
        }
        btnDelete.setOnAction(e -> {
            handleDeleteAuctionAction(id, card, (confirmedCardNode) -> {
                if (onActionClick != null) {
                    // Trả cả đối tượng item dữ liệu và node giao diện card về Controller
                    onActionClick.accept(item, confirmedCardNode);
                }
            });
        });
        actions.getChildren().addAll(btnEdit, btnDelete);

        // Lắp ráp toàn bộ card
        content.getChildren().addAll(nameDesc, priceContainer, attrBox, actions);
        card.getChildren().addAll(header, imgStack, content);

        return card;
    }

    /**
     * Xử lý hiển thị hộp thoại xác nhận trước khi thực hiện xóa sản phẩm.
     * Nếu người dùng đồng ý, hàm gọi lại (callback) onDeleteSuccess sẽ được kích hoạt để thực hiện hành động xóa.
     *
     * @param auctionId Mã định danh sản phẩm cần xóa
     * @param cardNode Đối tượng giao diện thẻ sản phẩm tương ứng trên giao diện người dùng
     * @param onDeleteSuccess Hàm callback nhận tham số là VBox thẻ sản phẩm khi người dùng xác nhận xóa thành công
     */
    public static void handleDeleteAuctionAction(String auctionId, VBox cardNode, Consumer<VBox> onDeleteSuccess) {
        boolean isConfirmed = NotificationController.showConfirmation(
                "Xác nhận xóa",
                "Bạn có chắc chắn muốn xóa sản phẩm này không?",
                "Hành động này không thể hoàn tác ID: " + auctionId,
                "Đồng ý",
                "Hủy bỏ"
        );
        // Nếu nhấn "Đồng ý" và có đăng ký hàm xử lý thành công từ Controller
        if (isConfirmed && onDeleteSuccess != null) {
            onDeleteSuccess.accept(cardNode); // Chuyển trả node Card này về cho Controller tự xóa
        }
    }

}
