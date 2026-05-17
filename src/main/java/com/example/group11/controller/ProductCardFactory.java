package com.example.group11.controller;

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

import java.util.function.Consumer;

public class ProductCardFactory {

    public static VBox createProductCard(String id, String name, String desc, String startPrice, String attributeKey,
                                         String attributeValue, String status, String imageUrl, Consumer<VBox> onDeleteSuccess) {
        // Container chính (mainCardContainer trong FXML)
        VBox card = new VBox();
        card.setPrefWidth(260.0);
        card.setMaxWidth(260.0);
        card.setStyle("-fx-background-color: #1A1A1A; -fx-border-color: #262626; -fx-border-width: 1; -fx-background-radius: 15; -fx-border-radius: 15;");

        // 1. Header: ID, Status
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
        Label lblStatus = new Label(status);
        lblStatus.setStyle("-fx-text-fill: #00e475; -fx-font-weight: bold;");
        lblStatus.setFont(new Font(9.0));
        statusBox.getChildren().addAll(dot, lblStatus);

        header.getChildren().addAll(lblId, spacer, statusBox);

        // 2. Hình ảnh sản phẩm
        StackPane imgStack = new StackPane();
        imgStack.setPrefHeight(135.0);
        imgStack.setStyle("-fx-background-color: #000000;");

        ImageView imgView = new ImageView();
        imgView.setFitHeight(135.0);
        imgView.setFitWidth(260.0);
        imgView.setPreserveRatio(true);
        // Thêm logic load ảnh từ URL nếu có, nếu không dùng ảnh mặc định
        try {
            imgView.setImage(new Image(imageUrl, true));
        } catch (Exception e) {
            imgView.setImage(new Image("https://lh3.googleusercontent.com/aida-public/AB6AXuBVMAGKkFbo45OTsBsSobXfbNDExErUNOrmwi5tBP3M_Xkz5Pp87L2CMVZm7fuR54EIXTbqY1PfIOd7C-1qaYKZ91Ycjjb2VeoXOM5ZMcwEWh9sRD1NBZMmwemftfNIADQcw5yHuueYdwrYntl4qm5r06zY4x9gCBJASSvhyOqt1L1yzlxfez9H_HhbLRRC2vpCAFBuAW3AMp0ZjZu-NDi1eteCstkcdYUG5Ysm7gsRCk3JzbdraApIRPHxNIWevRgwQ29qkB7xW4Y"));
        }
        imgStack.getChildren().add(imgView);

        // 3. Nội dung thông tin (VBox chính bên dưới ảnh)
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
        Label vStart = new Label(startPrice + " đ");
        vStart.setStyle("-fx-text-fill: rgba(255,255,255,0.8);");
        vStart.setFont(new Font(11.0));
        startPriceBox.getChildren().addAll(tStart, vStart);

        priceContainer.getChildren().addAll(startPriceBox);

        // Nút bấm hành động
        HBox actions = new HBox(8.0);
        Button btnDetails = new Button("CHI TIẾT");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDetails, javafx.scene.layout.Priority.ALWAYS);
        btnDetails.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #555555; -fx-border-radius: 5; -fx-padding: 10; -fx-cursor: hand;");
        btnDetails.setFont(new Font(11.0));

        Button btnDelete = new Button("XÓA");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDelete, javafx.scene.layout.Priority.ALWAYS);
        btnDelete.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 5; -fx-cursor: hand;");
        btnDelete.setFont(new Font(11.0));

        // Gán sự kiện xóa từ controller
        btnDelete.setOnAction(e -> handleDeleteAuctionAction(id, card, onDeleteSuccess));

        actions.getChildren().addAll(btnDetails, btnDelete);

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
