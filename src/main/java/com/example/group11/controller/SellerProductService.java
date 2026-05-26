package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.network.CreateAuctionPayload;
import com.auction.network.CreateItemPayload;
import com.auction.network.RequestType;
import com.auction.network.Response;
import com.auction.network.UpdateItemPayload;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellerProductService {

    public static void loadMyListingView(SellerAuctionListController controller) {
        controller.contentGrid.getChildren().clear();
        CalculatorView.updateCount(controller.totalProductsLabel, 0);

        Label loadingLabel = new Label("Đang kết nối máy chủ để tải danh sách...");
        loadingLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-style: italic;");
        controller.contentGrid.add(loadingLabel, 0, 0);

        System.out.println("Đang gửi yêu cầu lấy danh sách sản phẩm cá nhân và phiên đấu giá từ Server ở Luồng Ngầm...");

        Task<Map<String, Response>> fetchTask = new Task<>() {
            @Override
            protected Map<String, Response> call() throws Exception {
                Response itemsRes = ServerConnection.getInstance().send(RequestType.GET_MY_ITEMS, null);
                Response auctionsRes = ServerConnection.getInstance().send(RequestType.GET_AUCTIONS, null);
                Map<String, Response> map = new HashMap<>();
                map.put("items", itemsRes);
                map.put("auctions", auctionsRes);
                return map;
            }
        };

        fetchTask.setOnSucceeded(event -> {
            controller.contentGrid.getChildren().remove(loadingLabel);
            Map<String, Response> results = fetchTask.getValue();
            Response itemsResponse = results.get("items");
            Response auctionsResponse = results.get("auctions");

            if (itemsResponse != null && itemsResponse.isSuccess()) {
                controller.auctionItems = (List<Item>) itemsResponse.getData();
                controller.itemAuctionMap.clear();

                if (auctionsResponse != null && auctionsResponse.isSuccess()) {
                    List<Auction> auctions = (List<Auction>) auctionsResponse.getData();
                    if (auctions != null) {
                        for (Auction auction : auctions) {
                            if (auction.getItem() != null) {
                                controller.itemAuctionMap.put(auction.getItem().getId(), auction);
                            }
                        }
                    }
                }

                controller.applyFiltersAndSort();
                controller.updateHeaderRevenue();
            } else {
                String errorMsg = (itemsResponse != null) ? itemsResponse.getMessage() : "Không thể kết nối tới máy chủ!";
                NotificationController.showError(
                        "Lỗi tải dữ liệu",
                        "Hệ thống gặp sự cố khi đồng bộ danh sách sản phẩm.\nChi tiết: " + errorMsg
                );
            }
        });

        fetchTask.setOnFailed(e -> {
            controller.contentGrid.getChildren().remove(loadingLabel);
            NotificationController.showError("Lỗi hệ thống", "Đã xảy ra lỗi bất ngờ khi tải dữ liệu từ luồng nền.");
        });

        Thread thread = new Thread(fetchTask);
        thread.setDaemon(true);
        thread.start();
    }
}
