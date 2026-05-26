package com.example.group11.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * Lớp trợ giúp (Helper) quản lý trạng thái hiển thị giao diện đấu giá (Auction UI).
 * Cung cấp các phương thức điều hướng tab, chuyển đổi view, và định dạng trạng thái nút bấm.
 */
public class AuctionUIHelper {
    @FXML
    public static Button btnAnalytics;

    @FXML
    public static Button btnMyListings;

    @FXML
    public static Button btnOrderHistory;

    @FXML
    public static Button btnSettings;

    @FXML
    public static VBox myListingsView;

    @FXML
    public static VBox orderHistoryView;

    @FXML
    public static VBox registerProductView;

    @FXML
    public static VBox analyticsPane;

    public static boolean needsRefresh = true;

    /**
     * Thực thi logic tương ứng với từng tab điều hướng khi người bán chuyển tab.
     *
     * @param buttonId    ID của nút/tab được chọn
     * @param contentGrid Lưới chứa danh sách sản phẩm
     * @param controller  Bộ điều khiển màn hình người bán
     */
    public static void executeTabLogic(String buttonId, GridPane contentGrid, SellerAuctionListController controller) {

        if (controller == null) return;

        switch (buttonId) {
            case "btnMyListings":
                if (contentGrid.getChildren().isEmpty() || needsRefresh) {
                    controller.loadMyListingView();
                    needsRefresh = false;
                }
                break;
            case "btnAnalytics":
                controller.loadAnalyticsData();
                break;
            case "btnOrderHistory":
                controller.loadOrderHistory();
                break;
            case "btnSettings":
                controller.loadProfileData();
                break;
        }
    }

    /**
     * Thiết lập trạng thái có cần làm mới dữ liệu hay không.
     *
     * @param refresh true nếu cần làm mới, ngược lại là false
     */
    public static void setNeedsRefresh(boolean refresh) {
        needsRefresh = refresh;
    }

    /**
     * Hiển thị một Pane và ẩn tất cả các Pane khác trong StackPane để tránh xung đột giao diện.
     *
     * @param viewToShow VBox cần được hiển thị
     * @param allViews   Danh sách chứa tất cả các VBox giao diện
     */
    public static void showView(VBox viewToShow, List<VBox> allViews) {
        if (allViews == null) return;
        for (VBox view : allViews) {
            if (view != null) {
                boolean isTarget = (view == viewToShow);
                view.setVisible(isTarget);
                view.setManaged(isTarget);
            }
        }
    }

    /**
     * Thiết lập lại (Reset) style mặc định (không được chọn) cho toàn bộ các nút điều hướng.
     *
     * @param allButtons Danh sách toàn bộ các nút điều hướng cần reset
     */
    public static void resetAllButtons(List<Button> allButtons) {
        if (allButtons == null) return;
        String inactiveStyle = "-fx-background-color: transparent; " +
                "-fx-text-fill: #94A3B8; " +
                "-fx-font-weight: bold; " +
                "-fx-border-width: 0;";

        for (Button btn : allButtons) {
            if (btn != null) {
                btn.setStyle(inactiveStyle);
            }
        }
    }

    /**
     * Thiết lập style nổi bật (Active) cho nút đang được chọn.
     *
     * @param button Nút cần làm nổi bật
     */
    public static void setActiveStyle(Button button) {
        String activeStyle = "-fx-background-color: #112240; " +
                "-fx-text-fill: #FFD700; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: transparent #FFD700 transparent transparent; " +
                "-fx-border-width: 0 4 0 0;";
        button.setStyle(activeStyle);
    }

    /**
     * Lấy VBox tương ứng dựa trên mã định danh (ID) của nút điều hướng.
     *
     * @param id      ID của nút điều hướng
     * @param viewMap Bản đồ liên kết ID nút với VBox giao diện tương ứng
     * @return VBox tương ứng hoặc null nếu không tìm thấy
     */
    public static VBox getVBoxFromId(String id, Map<String, VBox> viewMap) {
        if (viewMap == null) return null;
        return viewMap.get(id);
    }

    /**
     * Tìm nút đang ở trạng thái kích hoạt (Active) dựa trên style màu chữ của nút.
     *
     * @param allButtons Danh sách tất cả nút điều hướng
     * @return Nút đang được chọn (Active) hoặc null nếu không có nút nào
     */
    public static Button findActiveButton(List<Button> allButtons) {
        if (allButtons == null) return null;
        for (Button btn : allButtons) {
            if (btn != null && btn.getStyle() != null && btn.getStyle().contains("#FFD700")) {
                return btn;
            }
        }
        return null;
    }
}
