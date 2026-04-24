package com.example.group11.controller;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private VBox boxContainer;


    @FXML
    private VBox coinContainer;

    @FXML
    private Button createAccount;

    @FXML
    private TextField enterEmail;

    @FXML
    private PasswordField enterPassword;

    @FXML
    private Hyperlink forgotPassword;

    @FXML
    private Button login;

    @FXML
    private CheckBox rememberPassword;

    @FXML
    private Line scaleBar;

    @FXML
    private Group scaleGroup;

    @FXML
    private HBox supportFunction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startEquilibriumAnimation();
    }

    private void startEquilibriumAnimation() {
        Duration speed = Duration.seconds(1.8);

        // 1. Thanh cân bập bênh (Góc âm là nghiêng trái xuống)
        RotateTransition rotateBar = new RotateTransition(speed, scaleBar);
        rotateBar.setFromAngle(-10);
        rotateBar.setToAngle(10);
        rotateBar.setCycleCount(Animation.INDEFINITE);
        rotateBar.setAutoReverse(true);
        rotateBar.setInterpolator(Interpolator.EASE_BOTH);
        rotateBar.play();

        // 2. Khối Hộp (Bên trái) - Khớp thứ tự: Nghiêng trái xuống -> Hộp đi xuống
        TranslateTransition animBox = new TranslateTransition(speed, boxContainer);
        animBox.setFromY(20);  // Giảm biên độ xuống 20
        animBox.setToY(-20);
        animBox.setCycleCount(Animation.INDEFINITE);
        animBox.setAutoReverse(true);
        animBox.setInterpolator(Interpolator.EASE_BOTH);
        animBox.play();

        // 3. Khối Tiền (Bên phải) - Đảo ngược lại
        TranslateTransition animCoin = new TranslateTransition(speed, coinContainer);
        animCoin.setFromY(-35); // Giảm biên độ xuống 35 (vì thanh bên này dài hơn)
        animCoin.setToY(35);
        animCoin.setCycleCount(Animation.INDEFINITE);
        animCoin.setAutoReverse(true);
        animCoin.setInterpolator(Interpolator.EASE_BOTH);
        animCoin.play();
    }
}
