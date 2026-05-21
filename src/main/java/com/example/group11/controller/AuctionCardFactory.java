package com.example.group11.controller;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

public class AuctionCardFactory {

    public static VBox createAuctionCard(Auction auction, Consumer<Auction> onBidAction, Consumer<Auction> onViewDetailsAction) {
        Item item = auction.getItem();
        if (item == null) return new VBox();

        // 1. Container chính của Card (Tương đương VBox fx:id="mainCardContainer")
        VBox cardContainer = new VBox();
        cardContainer.setMaxWidth(350.0);
        cardContainer.setPrefWidth(320.0);
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
        headerBar.setPadding(new Insets(10, 15, 5, 15));

        Label auctionIdLabel = new Label("ID: #" + auction.getId());
        auctionIdLabel.setTextFill(Color.valueOf("#94A3B8"));
        auctionIdLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        // Khoảng trống đẩy trạng thái về bên phải
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        // Container trạng thái (Dot + Text)
        HBox statusContainer = new HBox();
        statusContainer.setAlignment(Pos.CENTER_LEFT);
        statusContainer.setSpacing(6.0);

        Circle statusDot = new Circle(4.0);
        Label statusLabel = new Label(auction.getStatus().toString());
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        // Cấu hình màu sắc theo trạng thái
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            statusDot.setFill(Color.valueOf("#00e475"));
            statusLabel.setTextFill(Color.valueOf("#00e475"));
        } else {
            statusDot.setFill(Color.valueOf("#94A3B8"));
            statusLabel.setTextFill(Color.valueOf("#94A3B8"));
        }
        statusContainer.getChildren().addAll(statusDot, statusLabel);
        headerBar.getChildren().addAll(auctionIdLabel, spacerHeader, statusContainer);


        // --- BODY CONTAINER (Thông tin sản phẩm) ---
        VBox bodyContainer = new VBox();
        bodyContainer.setSpacing(12.0);
        bodyContainer.setPadding(new Insets(10, 15, 15, 15));

        // Tên sản phẩm
        Label productNameLabel = new Label(item.getName());
        productNameLabel.setTextFill(Color.WHITE);
        productNameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        productNameLabel.setWrapText(true);

        // Mô tả sản phẩm
        Label descriptionLabel = new Label(item.getDescription());
        descriptionLabel.setTextFill(Color.valueOf("#94A3B8"));
        descriptionLabel.setFont(Font.font("System", 13));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMinHeight(36.0); // Giữ khoảng trống đều nhau giữa các card

        // --- KHU VỰC HIỂN THỊ GIÁ ---
        HBox priceContainer = new HBox();
        priceContainer.setSpacing(15.0);

        // Giá khởi điểm
        VBox startPriceBox = new VBox(4);
        Label lblStartTitle = new Label("Giá khởi điểm");
        lblStartTitle.setTextFill(Color.valueOf("#64748B"));
        lblStartTitle.setFont(Font.font("System", 11));
        Label startingPriceLabel = new Label(String.format("%.2f $", item.getStartingPrice()));
        startingPriceLabel.setTextFill(Color.valueOf("#CBD5E1"));
        startingPriceLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        startPriceBox.getChildren().addAll(lblStartTitle, startingPriceLabel);

        // Giá hiện tại
        VBox currentPriceBox = new VBox(4);
        Label lblCurrentTitle = new Label("Giá hiện tại");
        lblCurrentTitle.setTextFill(Color.valueOf("#FFD700"));
        lblCurrentTitle.setFont(Font.font("System", 11));
        Label currentPriceLabel = new Label(String.format("%.2f $", auction.getCurrentHighestBid()));
        currentPriceLabel.setTextFill(Color.valueOf("#FFD700"));
        currentPriceLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        currentPriceBox.getChildren().addAll(lblCurrentTitle, currentPriceLabel);

        priceContainer.getChildren().addAll(startPriceBox, currentPriceBox);


        // --- KHU VỰC THỜI GIAN ---
        HBox timeContainer = new HBox();
        timeContainer.setSpacing(15.0);

        // Bắt đầu
        VBox startBox = new VBox(4);
        Label lblStart = new Label("Bắt đầu");
        lblStart.setTextFill(Color.valueOf("#64748B"));
        lblStart.setFont(Font.font("System", 11));
        String startTimeStr = auction.getStartTime() != null ? auction.getStartTime().toString() : "--:--";
        Label startTimeLabel = new Label(startTimeStr);
        startTimeLabel.setTextFill(Color.WHITE);
        startTimeLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        startBox.getChildren().addAll(lblStart, startTimeLabel);

        // Kết thúc
        VBox endBox = new VBox(4);
        Label lblEnd = new Label("Kết thúc");
        lblEnd.setTextFill(Color.valueOf("#64748B"));
        lblEnd.setFont(Font.font("System", 11));
        String endTimeStr = auction.getEndTime() != null ? auction.getEndTime().toString() : "--:--";
        Label endTimeLabel = new Label(endTimeStr);
        endTimeLabel.setTextFill(Color.WHITE);
        endTimeLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        endBox.getChildren().addAll(lblEnd, endTimeLabel);

        timeContainer.getChildren().addAll(startBox, endBox);


        // --- HÀNH ĐỘNG BUTTONS ---
        VBox actionBox = new VBox(8.0);
        actionBox.setPadding(new Insets(5, 0, 0, 0));

        // Nút ĐẶT GIÁ
        Button bidButton = new Button("ĐẶT GIÁ");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setFont(Font.font("System", FontWeight.BOLD, 13));

        // Thiết lập trạng thái và màu sắc kích hoạt nút ĐẶT GIÁ
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            bidButton.setDisable(false);
            bidButton.setStyle("-fx-background-color: #ffd700; -fx-text-fill: #3a3000; -fx-background-radius: 6; -fx-padding: 10; -fx-cursor: hand;");
        } else {
            bidButton.setDisable(true);
            bidButton.setStyle("-fx-background-color: #262626; -fx-text-fill: #555555; -fx-background-radius: 6; -fx-padding: 10;");
        }

        // Nút CHI TIẾT
        Button detailsButton = new Button("CHI TIẾT");
        detailsButton.setMaxWidth(Double.MAX_VALUE);
        detailsButton.setFont(Font.font("System", FontWeight.BOLD, 13));
        detailsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10; -fx-cursor: hand;");

        // Gắn sự kiện (Callbacks)
        bidButton.setOnAction(e -> {
            if (onBidAction != null) onBidAction.accept(auction);
        });

        detailsButton.setOnAction(e -> {
            if (onViewDetailsAction != null) onViewDetailsAction.accept(auction);
        });

        actionBox.getChildren().addAll(bidButton, detailsButton);

        // Gom tất cả vào Body, rồi gom Body + Header vào Card chính
        bodyContainer.getChildren().addAll(productNameLabel, descriptionLabel, priceContainer, timeContainer, actionBox);
        cardContainer.getChildren().addAll(headerBar, bodyContainer);

        return cardContainer;
    }
}







