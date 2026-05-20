package com.example.group11.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class ItemBidderController implements Initializable {

    @FXML
    private Label auctionIdLabel;

    @FXML
    private Button bidButton;

    @FXML
    private Label currentPriceLabel;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
