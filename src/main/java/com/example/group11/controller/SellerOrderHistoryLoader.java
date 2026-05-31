package com.example.group11.controller;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trình nạp Lịch sử đơn hàng của Người bán (Seller Order History Loader).
 * Lớp này chịu trách nhiệm khởi tạo cấu trúc hiển thị cột của TableView đơn hàng, định dạng màu sắc trạng thái,
 * và nạp dữ liệu lịch sử các giao dịch hoàn thành của Người bán từ danh sách sản phẩm.
 * Lớp này được thiết kế như một Utility Class, không có hàm khởi tạo và chỉ chứa các phương thức tĩnh.
 */
public class SellerOrderHistoryLoader {

    /**
     * Tải và hiển thị danh sách lịch sử đơn hàng của người bán.
     * Phương thức này thực hiện lọc danh sách các phiên đấu giá đã kết thúc (FINISHED hoặc PAID) của Người bán,
     * thiết lập bộ lọc tìm kiếm theo thời gian thực (realtime) dựa trên TextField, và đẩy dữ liệu lên TableView.
     *
     * @param auctionItems Danh sách toàn bộ sản phẩm của Người bán
     * @param itemAuctionMap Bản đồ liên kết ID sản phẩm với thông tin phiên đấu giá tương ứng
     * @param orderTable Bảng TableView hiển thị danh sách lịch sử đơn hàng
     * @param searchOrderField Ô nhập liệu tìm kiếm bộ lọc đơn hàng
     */
    public static void loadOrderHistory(
            List<Item> auctionItems,
            Map<Integer, Auction> itemAuctionMap,
            TableView<Order> orderTable,
            TextField searchOrderField) {

        List<Order> orders = new ArrayList<>();
        if (auctionItems != null && itemAuctionMap != null) {
            for (Item item : auctionItems) {
                Auction auction = itemAuctionMap.get(item.getId());
                if (auction != null && (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID)
                        && auction.getCurrentWinner() != null) {
                    orders.add(new Order(
                            auction.getId(),
                            item.getName(),
                            auction.getCurrentWinner().getUsername(),
                            auction.getCurrentHighestBid(),
                            auction.getEndTime(),
                            auction.getStatus().name()
                    ));
                }
            }
        }

        ObservableList<Order> orderList = FXCollections.observableArrayList(orders);
        FilteredList<Order> filteredData = new FilteredList<>(orderList, p -> true);

        if (searchOrderField != null) {
            searchOrderField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(order -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    if (String.valueOf(order.getId()).contains(lowerCaseFilter)) {
                        return true;
                    } else if (order.getProduct().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (order.getBuyer().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (order.getStatus().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }
                    return false;
                });
            });
        }
        orderTable.setItems(filteredData);
    }

    /**
     * Khởi tạo cấu trúc các cột của bảng Lịch sử đơn hàng và thiết lập hiển thị/tô màu.
     * Cài đặt các CellValueFactory để liên kết thuộc tính, và CellFactory để định dạng tùy biến
     * giao diện (Màu sắc trạng thái giao dịch PAID/FINISHED/CANCEL, tiền tệ $, và mốc thời gian).
     *
     * @param colOrderId Cột Mã đơn hàng
     * @param colProduct Cột Tên sản phẩm
     * @param colBuyer Cột Người thắng thầu (Người mua)
     * @param colPrice Cột Giá chốt đơn hàng
     * @param colDate Cột Thời gian hoàn thành giao dịch
     * @param colStatus Cột Trạng thái đơn hàng (PAID, FINISHED, CANCELLED...)
     */
    public static void setupOrderTableColumns(
            TableColumn<Order, Integer> colOrderId,
            TableColumn<Order, String> colProduct,
            TableColumn<Order, String> colBuyer,
            TableColumn<Order, Double> colPrice,
            TableColumn<Order, LocalDateTime> colDate,
            TableColumn<Order, String> colStatus) {

        colOrderId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("product"));
        colBuyer.setCellValueFactory(new PropertyValueFactory<>("buyer"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colOrderId.setCellFactory(column -> new TableCell<Order, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("#" + item);
                    setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold;");
                }
            }
        });

        colProduct.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #FFFFFF;");
                }
            }
        });

        colBuyer.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #E2E8F0;");
                }
            }
        });

        colPrice.setCellFactory(column -> new TableCell<Order, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("$%,.2f", item));
                    setStyle("-fx-text-fill: #FFC107; -fx-font-weight: bold;");
                }
            }
        });

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        colDate.setCellFactory(column -> new TableCell<Order, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.format(formatter));
                    setStyle("-fx-text-fill: #94A3B8;");
                }
            }
        });

        colStatus.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toUpperCase());
                    if ("PAID".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold;");
                    } else if ("FINISHED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

}
