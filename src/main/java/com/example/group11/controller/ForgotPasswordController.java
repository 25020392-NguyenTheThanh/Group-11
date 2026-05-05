package com.example.group11.controller;

import com.auction.manager.UserManager;
import com.auction.model.user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private VBox mainCard;

    @FXML
    private VBox newPasswordBox;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private VBox usernameBox;

    @FXML
    private TextField usernameField;

    @FXML
    private Text welcomeText;

    private User currentUser;

    private UserManager userManager;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        userManager = UserManager.getInstance();

    }


    @FXML
    private void handleVerifyUsername(ActionEvent event) {
        String usernameInput = usernameField.getText().trim();

        currentUser = userManager.findUser(usernameInput);

        if (currentUser != null) {
            // Hiệu ứng chuyển đổi: Ẩn Box1, Hiện Box2
            usernameBox.setVisible(false);
            usernameBox.setManaged(false);

            newPasswordBox.setVisible(true);
            newPasswordBox.setManaged(true);

            welcomeText.setText("Chào " + currentUser.getUsername() + ", hãy nhập mật khẩu mới.");
        } else {
            showSimpleAlert("Lỗi", "Tên đăng nhập không tồn tại!");
        }
    }

    @FXML
    private void handleUpdatePassword(ActionEvent event) {

        String pass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (pass.isEmpty() || !pass.equals(confirm)) {
            showSimpleAlert("Lỗi", "Mật khẩu không khớp hoặc đang để trống!");
        }

        currentUser.setPassWord(pass);
        showSimpleAlert("Thành công", "Mật khẩu của bạn đã được thay đổi.");

        FXMLLoader loader = EquilibriumAnimation.changeScene(event, "login-view.fxml", "Welcome to auction floor!");
    }

    private void showSimpleAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void handleBackToLogin(ActionEvent event) {
        FXMLLoader loader = EquilibriumAnimation.changeScene(event, "login-view.fxml", "Welcome to auction floor!");
    }
}
