package com.example.group11.controller;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;

public class ProductFormManager {

    public static void setupCategoryMenuItems(SellerAuctionListController controller) {
        MenuItem menuItemElectronics = new MenuItem("Electronics");
        MenuItem menuItemVehicle = new MenuItem("Vehicle");
        MenuItem menuItemArt = new MenuItem("Art");

        menuItemElectronics.setOnAction(e -> handleCategorySelection(controller, "Electronics", "THƯƠNG HIỆU (BRAND)", "Ví dụ: ASUS, Apple, Samsung..."));
        menuItemVehicle.setOnAction(e -> handleCategorySelection(controller, "Vehicle", "NĂM SẢN XUẤT (YEAR)", "Ví dụ: 2024, 2025..."));
        menuItemArt.setOnAction(e -> handleCategorySelection(controller, "Art", "NGHỆ SĨ (ARTIST)", "Ví dụ: Leonardo da Vinci, Nguyễn Phan Chánh..."));

        controller.categoryMenuButton.getItems().setAll(menuItemElectronics, menuItemVehicle, menuItemArt);
    }

    public static void handleCategorySelection(SellerAuctionListController controller, String categoryName, String labelText, String promptText) {
        controller.categoryMenuButton.setText(categoryName);

        if (controller.dynamicAttributesContainer != null) {
            controller.dynamicAttributesContainer.getChildren().clear();

            VBox fieldGroup = new VBox(5.0);

            Label dynamicLabel = new Label(labelText.toUpperCase());
            dynamicLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-weight: bold;");

            TextField dynamicTextField = new TextField();
            dynamicTextField.setPromptText(promptText);
            dynamicTextField.setPrefHeight(45.0);
            dynamicTextField.setStyle("-fx-background-color: #0A192F; -fx-text-fill: white; -fx-border-color: #1E2D45; -fx-border-radius: 8; -fx-background-radius: 8;");
            dynamicTextField.setId("customAttributeField");

            fieldGroup.getChildren().addAll(dynamicLabel, dynamicTextField);
            controller.dynamicAttributesContainer.getChildren().add(fieldGroup);
        }
    }

    public static void handleStartEditProduct(SellerAuctionListController controller, Item item) {
        controller.editingItem = item;

        controller.productNameField.setText(item.getName());
        controller.startingPriceField.setText(String.valueOf(item.getStartingPrice()));
        controller.descriptionArea.setText(item.getDescription());

        String category = item.getCategory();
        controller.categoryMenuButton.setText(category);

        controller.dynamicAttributesContainer.getChildren().clear();
        String labelText = "";
        String promptText = "";
        String attributeValue = "";

        if (item instanceof Art art) {
            labelText = "NGHỆ SĨ (ARTIST)";
            promptText = "Ví dụ: Leonardo da Vinci, Nguyễn Phan Chánh...";
            attributeValue = art.getArtist();
        } else if (item instanceof Electronics elec) {
            labelText = "THƯƠNG HIỆU (BRAND)";
            promptText = "Ví dụ: ASUS, Apple, Samsung...";
            attributeValue = elec.getBrand();
        } else if (item instanceof Vehicle veh) {
            labelText = "NĂM SẢN XUẤT (YEAR)";
            promptText = "Ví dụ: 2024, 2025...";
            attributeValue = String.valueOf(veh.getYear());
        }

        if (!labelText.isEmpty()) {
            handleCategorySelection(controller, category, labelText, promptText);
            TextField customField = (TextField) controller.dynamicAttributesContainer.lookup("#customAttributeField");
            if (customField != null) {
                customField.setText(attributeValue);
            }
        }

        controller.minimumBidIncrementField.setDisable(true);
        controller.minimumBidIncrementField.setText("");
        controller.startDatePicker.setDisable(true);
        controller.startDatePicker.setValue(null);
        controller.endDatePicker.setDisable(true);
        controller.endDatePicker.setValue(null);

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            controller.linkImageUrl = item.getImageUrl();
            controller.selectedImageFile = null;

            try {
                java.io.File imgFile = new java.io.File("src/main/resources" + item.getImageUrl());
                if (imgFile.exists()) {
                    ImagesController.displayImage(imgFile, controller.productImageView, controller.uploadPrompt);
                } else {
                    controller.productImageView.setImage(new Image("https://placehold.co/260x135/000000/FFFFFF/png?text=No+Image"));
                    controller.productImageView.setVisible(true);
                    controller.uploadPrompt.setVisible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            controller.productImageView.setImage(null);
            controller.productImageView.setVisible(false);
            controller.uploadPrompt.setVisible(true);
            controller.linkImageUrl = null;
            controller.selectedImageFile = null;
        }

        controller.registerTitleLabel.setText("SỬA THÔNG TIN SẢN PHẨM");
        controller.registerSubtitleLabel.setText("Thay đổi thông tin cho sản phẩm ID: " + item.getId());
        controller.submitButton.setText("LƯU THAY ĐỔI");

        controller.lastView = controller.currentView;
        controller.lastButton = AuctionUIHelper.findActiveButton(controller.allButtons);
        AuctionUIHelper.resetAllButtons(controller.allButtons);
        controller.currentView = controller.registerProductView;
        AuctionUIHelper.showView(controller.registerProductView, controller.allViews);
    }

    public static void clearRegistrationForm(SellerAuctionListController controller) {
        controller.productNameField.clear();
        controller.startingPriceField.clear();
        controller.minimumBidIncrementField.clear();
        controller.descriptionArea.clear();

        controller.categoryMenuButton.setText("Chọn danh mục");

        controller.startDatePicker.setValue(null);
        controller.endDatePicker.setValue(null);

        controller.selectedImageFile = null;
        controller.productImageView.setImage(null);
        controller.productImageView.setVisible(false);
        controller.uploadPrompt.setVisible(true);
        controller.linkImageUrl = null;
    }
}
