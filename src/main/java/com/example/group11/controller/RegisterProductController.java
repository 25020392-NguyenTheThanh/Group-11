package com.example.group11.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class RegisterProductController implements Initializable {
    @FXML
    private Button addImageButton;

    @FXML
    private MenuButton categoryMenuButton;

    @FXML
    private TextField depositField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField minimumBidIncrementField;

    @FXML
    private TextField productNameField;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField startingPriceField;

    @FXML
    private Button submitButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupMenuButtonUpdate(categoryMenuButton);

    }

    //  Tự động cập nhật nhãn (Text) của MenuButton khi người dùng chọn một Item bên trong
    private void setupMenuButtonUpdate(MenuButton menuButton) {
        for (MenuItem item : menuButton.getItems()) {
            item.setOnAction(event -> {
                menuButton.setText(item.getText());
                // Gọi logic lọc dữ liệu tại đây
                System.out.println("Danh mục đang lọc theo: " + item.getText());
            });
        }
    }

    @FXML
    void handleBackAuction(ActionEvent event) {
        FXMLLoader loader = EquilibriumAnimation.changeScene(event, "sellerAuctionList-view.fxml", "Auction floor of Seller");

        if (loader != null) {
            SellerAuctionListController controller = loader.getController();
        }
    }

    @FXML
    void handleUploadImage() {

    }
}
