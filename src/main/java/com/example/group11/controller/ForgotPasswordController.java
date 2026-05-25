package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.network.RequestType;
import com.auction.network.Response;
import com.auction.network.ResetPasswordPayload;
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
        if (usernameInput.isEmpty()) {
            NotificationController.showAlert("Lỗi", "Vui lòng nhập tên đăng nhập!");
            return;
        }

        ServerConnection connection = ServerConnection.getInstance();
        if (!connection.isConnected()) {
            try {
                System.out.println("Đang kết nối lại tới Server...");
                connection.connect();
            } catch (Exception ex) {
                NotificationController.showAlert("Lỗi kết nối", "Không thể kết nối đến máy chủ: " + ex.getMessage());
                return;
            }
        }

        Response response = connection.send(RequestType.VERIFY_USERNAME, usernameInput);
        if (response != null && response.isSuccess()) {
            currentUser = (User) response.getData();
            // Hiệu ứng chuyển đổi: Ẩn Box1, Hiện Box2
            usernameBox.setVisible(false);
            usernameBox.setManaged(false);

            newPasswordBox.setVisible(true);
            newPasswordBox.setManaged(true);

            welcomeText.setText("Chào " + currentUser.getUsername() + ", hãy nhập mật khẩu mới.");
        } else {
            String errorMsg = (response != null) ? response.getMessage() : "Tên đăng nhập không tồn tại!";
            NotificationController.showAlert("Lỗi", errorMsg);
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
            return;
        }

        if (pass.length() < 4) {
            NotificationController.showAlert("Lỗi", "Mật khẩu mới phải có ít nhất 4 ký tự!");
            return;
        }

        ResetPasswordPayload payload = new ResetPasswordPayload(currentUser.getUsername(), pass);
        Response response = ServerConnection.getInstance().send(RequestType.RESET_PASSWORD, payload);
        if (response != null && response.isSuccess()) {
            NotificationController.showNotification("Thành công", "Mật khẩu của bạn đã được thay đổi.");
            FXMLLoader loader = GenerationSupport.changeScene(event, "login-view.fxml", "Welcome to auction floor!");
        } else {
            String errorMsg = (response != null) ? response.getMessage() : "Không thể cập nhật mật khẩu mới!";
            NotificationController.showAlert("Lỗi", errorMsg);
        }
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