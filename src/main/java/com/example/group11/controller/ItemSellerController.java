package com.example.group11.controller;

import com.auction.manager.ItemManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.example.group11.controller.ForgotPasswordController.showSimpleAlert;

public class ItemSellerController implements Initializable {

    @FXML
    private Label auctionIdLabel;

    @FXML
    private Label currentPriceLabel;

    @FXML
    private Button deleteButton;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Button detailsButton;

    @FXML
    private Label endTimeLabel;

    @FXML
    private StackPane imageContainer;

    @FXML
    private VBox mainCardContainer;

    @FXML
    private ImageView productImage;

    @FXML
    private Label productNameLabel;

    @FXML
    private Label startTimeLabel;

    @FXML
    private Label startingPriceLabel;

    @FXML
    private HBox statusContainer;

    @FXML
    private Circle statusDot;

    @FXML
    private Label statusLabel;

    @FXML
    private Label timerLabel;

    private ItemManager itemManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    private void handleDeleteAction() {
        // 1. Tạo một Alert xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Bạn có chắc chắn muốn xóa đấu giá này không?");
        alert.setContentText("Hành động này không thể hoàn tác: " + auctionIdLabel.getText());

        // 2. Tùy chỉnh nút (nếu muốn tiếng Việt)
        ButtonType buttonTypeYes = new ButtonType("Đồng ý");
        ButtonType buttonTypeNo = new ButtonType("Hủy bỏ");
        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

        // 3. Hiển thị và chờ phản hồi từ người dùng
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == buttonTypeYes) {
            // Thực hiện logic xóa ở đây
            performDelete();
        } else {
            // Người dùng chọn Hủy hoặc đóng cửa sổ
            System.out.println("Đã hủy lệnh xóa.");
        }
    }

    private void performDelete() {
        // Logic gọi Database hoặc API để xóa item
        System.out.println("Đang xóa mục: " + auctionIdLabel.getText());

        // Ví dụ: Thông báo sau khi xóa thành công
        showSimpleAlert("Thông báo", "Đã xóa thành công!");

        // Có thể thêm logic ẩn/xóa mainCardContainer khỏi UI cha tại đây
    }
}
