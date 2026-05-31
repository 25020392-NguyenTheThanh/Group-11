package com.example.group11.controller;

import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.network.RequestType;
import com.auction.network.Response;
import com.auction.client.ServerConnection;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trình nạp giao diện Danh sách sản phẩm của Người bán (Seller My Listings Loader).
 * Lớp này chịu trách nhiệm gửi yêu cầu lấy danh sách sản phẩm và các phiên đấu giá tương ứng từ Server,
 * sau đó thông báo kết quả qua Callback để đồng bộ lại giao diện người dùng.
 * Lớp này được thiết kế như một Utility Class, không có hàm khởi tạo và chỉ chứa các phương thức tĩnh.
 */
public class SellerMyListingLoader {

    /**
     * Giao diện callback để nhận kết quả nạp danh sách sản phẩm và đấu giá từ máy chủ.
     */
    public interface MyListingCallback {
        /**
         * Được kích hoạt khi nạp thành công dữ liệu từ máy chủ.
         * 
         * @param items Danh sách sản phẩm đã tải về thành công
         * @param itemAuctionMap Bản đồ liên kết ID sản phẩm với thông tin đấu giá tương ứng
         */
        void onSuccess(List<Item> items, Map<Integer, Auction> itemAuctionMap);
    }

    /**
     * Nạp danh sách sản phẩm cá nhân và các phiên đấu giá tương ứng từ Server ở chế độ bất đồng bộ.
     * Phương thức này thực hiện hiển thị thông báo tải dữ liệu tạm thời trên giao diện, tạo Thread
     * chạy ngầm để gửi yêu cầu mạng, sau đó đồng bộ kết quả về luồng UI chính thông qua Callback.
     *
     * @param contentGrid GridPane chứa các thẻ sản phẩm đấu giá hiển thị cho Người bán
     * @param totalProductsLabel Nhãn hiển thị tổng số lượng sản phẩm của Người bán
     * @param callback Callback xử lý kết quả dữ liệu nhận được sau khi tải thành công
     */
    public static void loadMyListingView(
            GridPane contentGrid,
            Label totalProductsLabel,
            MyListingCallback callback) {
        contentGrid.getChildren().clear();
        CalculatorView.updateCount(totalProductsLabel, 0);

        Label loadingLabel = new Label("Đang kết nối máy chủ để tải danh sách...");
        loadingLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-style: italic;");
        contentGrid.add(loadingLabel, 0, 0);

        System.out.println("Đang gửi yêu cầu lấy danh sách sản phẩm cá nhân và phiên đấu giá từ Server ở Luồng Ngầm...");

        Task<Map<String, Response>> fetchTask = new javafx.concurrent.Task<>() {
            @Override
            protected Map<String, Response> call() throws Exception {
                // Gửi yêu cầu lấy sản phẩm cá nhân và toàn bộ phiên đấu giá từ máy chủ
                Response itemsRes = ServerConnection.getInstance().send(RequestType.GET_MY_ITEMS, null);
                Response auctionsRes = ServerConnection.getInstance().send(RequestType.GET_AUCTIONS, null);
                Map<String, Response> map = new HashMap<>();
                map.put("items", itemsRes);
                map.put("auctions", auctionsRes);
                return map;
            }
        };

        fetchTask.setOnSucceeded(event -> {
            contentGrid.getChildren().remove(loadingLabel);
            Map<String, Response> results = fetchTask.getValue();
            Response itemsResponse = results.get("items");
            Response auctionsResponse = results.get("auctions");

            if (itemsResponse != null && itemsResponse.isSuccess()) {
                List<Item> fetchedItems = (List<Item>) itemsResponse.getData();
                if (fetchedItems == null) {
                    fetchedItems = new ArrayList<>();
                }
                Map<Integer, Auction> fetchedMap = new HashMap<>();

                if (auctionsResponse != null && auctionsResponse.isSuccess()) {
                    List<Auction> auctions = (List<Auction>) auctionsResponse.getData();
                    if (auctions != null) {
                        for (Auction auction : auctions) {
                            if (auction.getItem() != null) {
                                fetchedMap.put(auction.getItem().getId(), auction);
                            }
                        }
                    }
                }

                // Gọi callback để trả dữ liệu đã tải về cho controller
                callback.onSuccess(fetchedItems, fetchedMap);
            } else {
                String errorMsg = (itemsResponse != null) ? itemsResponse.getMessage() : "Không thể kết nối tới máy chủ!";
                NotificationController.showError(
                        "Lỗi tải dữ liệu",
                        "Hệ thống gặp sự cố khi đồng bộ danh sách sản phẩm.\nChi tiết: " + errorMsg
                );
            }
        });

        fetchTask.setOnFailed(e -> {
            contentGrid.getChildren().remove(loadingLabel);
            NotificationController.showError("Lỗi hệ thống", "Đã xảy ra lỗi bất ngờ khi tải dữ liệu từ luồng nền.");
        });

        Thread thread = new Thread(fetchTask);
        thread.setDaemon(true);
        thread.start();
    }
}
