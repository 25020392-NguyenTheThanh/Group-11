package com.example.group11.controller;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
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

public class ProductCardFactory {
    /**
     * Hàm tạo Card sản phẩm dùng chung (Cả Bidder và Seller đều dùng được)
     *  item Đối tượng Item polymorph (Art, Electronics, Vehicle) lấy từ DB
     *  isSeller Nếu là true thì hiện nút XÓA/SỬA, nếu là false (Bidder) thì hiện nút ĐẤU GIÁ
     *  onActionClick Hành động xử lý khi nhấn nút tương ứng
     */
    public static VBox createProductCard(Item item, boolean isSeller, BiConsumer<Item, VBox> onDetailsClick, BiConsumer<Item, VBox> onActionClick) {
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
        Circle dot = new Circle(3.0, javafx.scene.paint.Color.web("#00e475"));
        Label lblStatus = new Label("AVAILABLE");
        lblStatus.setStyle("-fx-text-fill: #00e475; -fx-font-weight: bold;");
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
        Label vStart = new Label(startPrice + " $");
        vStart.setStyle("-fx-text-fill: rgba(255,255,255,0.8);");
        vStart.setFont(new Font(11.0));
        startPriceBox.getChildren().addAll(tStart, vStart);

        priceContainer.getChildren().addAll(startPriceBox);
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

        // 3. THAY ĐỔI NÚT BẤM LINH HOẠT THEO VAI TRÒ (ROLE)

        if (isSeller) {
            // Giao diện phía Seller: Hiện nút quản lý
            Button btnDelete = new Button("XÓA ");
            btnDelete.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnDelete, javafx.scene.layout.Priority.ALWAYS);
            btnDelete.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8;");
            btnDelete.setOnAction(e -> {
                handleDeleteAuctionAction(id, card, (confirmedCardNode) -> {
                    if (onActionClick != null) {
                        // Trả cả đối tượng item dữ liệu và node giao diện card về Controller
                        onActionClick.accept(item, confirmedCardNode);
                    }
                });
            });
            actions.getChildren().add(btnDelete);
        } else {
            // Giao diện phía Bidder: Hiện nút tham gia đấu giá
            Button btnBid = new Button("RA GIÁ / ĐẤU GIÁ");
            btnBid.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnBid, javafx.scene.layout.Priority.ALWAYS);
            btnBid.setStyle("-fx-background-color: #ffd700; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8; -fx-cursor: hand;");
            btnBid.setOnAction(e -> {
                if (onActionClick != null) onActionClick.accept(item, card);
            });
            actions.getChildren().add(btnBid);
        }

        // Lắp ráp toàn bộ card
        content.getChildren().addAll(nameDesc, priceContainer, attrBox, actions);
        card.getChildren().addAll(header, imgStack, content);

        return card;
    }

    public static void handleDeleteAuctionAction(String auctionId, VBox cardNode, Consumer<VBox> onDeleteSuccess) {
        boolean isConfirmed = NotificationController.showConfirmation(
                "Xác nhận xóa",
                "Bạn có chắc chắn muốn xóa đấu giá này không?",
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
