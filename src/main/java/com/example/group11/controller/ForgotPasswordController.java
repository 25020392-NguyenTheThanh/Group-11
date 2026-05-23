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

/**
 * Bộ điều khiển giao diện khôi phục mật khẩu (Quên mật khẩu).
 * Quản lý các bước xác thực tài khoản và đổi mật khẩu mới cho người dùng.
 */
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


    /**
     * Khởi tạo bộ điều khiển khôi phục mật khẩu. Lấy instance của UserManager.
     *
     * @param location Vị trí tương đối của tài nguyên FXML
     * @param resources Các tài nguyên được bản địa hóa
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        userManager = UserManager.getInstance();

    }


    /**
     * Xử lý xác thực tên đăng nhập của người dùng.
     * Nếu tồn tại, chuyển sang màn hình nhập mật khẩu mới.
     *
     * @param event Sự kiện kích hoạt hành động từ nút bấm xác nhận
     */
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
            NotificationController.showAlert("Lỗi", "Tên đăng nhập không tồn tại!");
        }
    }

    /**
     * Xử lý cập nhật mật khẩu mới của người dùng vào hệ thống và chuyển lại về trang đăng nhập.
     *
     * @param event Sự kiện kích hoạt hành động từ nút lưu mật khẩu
     */
    @FXML
    private void handleUpdatePassword(ActionEvent event) {

        String pass    = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (pass.isEmpty() || !pass.equals(confirm)) {
            NotificationController.showAlert("Lỗi", "Mật khẩu không khớp hoặc đang để trống!");
            return;   // BUG CŨ: thiếu return -> vẫn tiếp tục dù mật khẩu sai
        }

        // Lưu mật khẩu mới (đã hash) xuống DB qua UserManager
        boolean ok = userManager.updatePassword(currentUser, pass);
        if (!ok) {
            NotificationController.showAlert("Lỗi", "Không thể cập nhật mật khẩu, vui lòng thử lại!");
            return;
        }

        NotificationController.showNotification("Thành công", "Mật khẩu của bạn đã được thay đổi.");
        FXMLLoader loader = GenerationSupport.changeScene(event, "login-view.fxml", "Welcome to auction floor!");
    }

    /**
     * Xử lý hành động quay lại trang đăng nhập khi bấm nút quay lại.
     *
     * @param event Sự kiện kích hoạt hành động quay lại
     */
    @FXML
    void handleBackToLogin(ActionEvent event) {
        FXMLLoader loader = GenerationSupport.changeScene(event, "login-view.fxml", "Welcome to auction floor!");
    }

}