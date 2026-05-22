package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.data.DataManager;
import com.auction.exception.AuthenticationException;
import com.auction.manager.UserManager;
import com.auction.model.user.User;
import com.auction.network.LoginPayload;
import com.auction.network.RegisterPayload;
import com.auction.network.RequestType;
import com.auction.network.Response;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;


public class LoginController implements Initializable {

    @FXML
    private VBox boxContainer;

    @FXML
    private VBox coinContainer;

    @FXML
    private Label createAccountLabel;

    @FXML
    private VBox door;

    @FXML
    private PasswordField enterPassword;

    @FXML
    private Hyperlink forgotPassword;

    @FXML
    private Button loginButton;

    @FXML
    private VBox loginDialog;

    @FXML
    private ImageView logoImage;

    @FXML
    private Hyperlink privacyLink;

    @FXML
    private Rectangle scaleBar;

    @FXML
    private Hyperlink termsLink;

    @FXML
    private TextField username;

    @FXML
    private HBox rootHBox;

    private UserManager userManager;

    @FXML
    private TextField visiblePassword;

    @FXML
    private Text togglePasswordIcon;

    private boolean isPasswordVisible = false;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Thiết lập mô hình chuyển động.
        LoginEffectHelper.startEquilibriumAnimation(scaleBar, boxContainer, coinContainer);

        // Thiết lập hiệu ứng gạch chân khi di chuột.
        LoginEffectHelper.setupHoverEffect(termsLink);
        LoginEffectHelper.setupHoverEffect(privacyLink);

        createAccountLabel.setOnMouseClicked(event -> showSignUp());

        userManager = UserManager.getInstance();

        visiblePassword.textProperty().bindBidirectional(enterPassword.textProperty());
    }

    @FXML
    void handleLogin(ActionEvent event) throws IOException {
        ServerConnection connection = ServerConnection.getInstance();
        String userName = username.getText().trim();
        String passWord = enterPassword.getText();

        // 1. Kiểm tra nếu để trống trường nhập liệu
        if (userName.isEmpty() || passWord.isEmpty()) {
            NotificationController.showAlert("Đăng nhập thất bại", "Vui lòng nhập đầy đủ cả tên đăng nhập và mật khẩu!");
            return;
        }

        LoginPayload payload = new LoginPayload(userName, passWord);

        // BẮT BUỘC: Nếu chưa kết nối hoặc kết nối cũ đã đóng (do đăng xuất) -> Kết nối lại
        if (!connection.isConnected()) {
            System.out.println("Đang kết nối lại tới Server...");
            connection.connect();
        }

        Response response = connection.send(RequestType.LOGIN, payload);
        if (response.isSuccess()) {
            ServerConnection.getInstance().startListening();
            User loggedInUser = (User) response.getData();

            String role = loggedInUser.getRole();
            FXMLLoader loader;

            switch (role) {
                case "BIDDER":
                    loader = GenerationSupport.changeScene(event, "bidderAuctionList-view.fxml", "Auction floor of Bidder");
                    break;

                case "SELLER":
                    loader = GenerationSupport.changeScene(event, "sellerAuctionList-view.fxml", "Auction floor of Seller");
                    SellerAuctionListController controller=loader.getController();
                    controller.setUser(loggedInUser);
                    break;
                default:
                    throw new AuthenticationException("Role " + role + " is not recognized.");
            }

            if (loader != null) {
                Object controller = loader.getController();
                if (controller instanceof BidderAuctionListController c) {
                    c.setUser(loggedInUser);
                } else if (controller instanceof SellerAuctionListController c) {
                    c.setUser(loggedInUser);
                } else {
                    NotificationController.showAlert("Lỗi khởi tạo", "Lỗi tải giao diện hệ thống!");
                }
            }
        } else {
            // BẮT BUỘC PHẢI CÓ: Hiển thị lỗi từ server trả về
            String errorMsg = (response != null) ? response.getMessage() : "Không thể kết nối đến máy chủ!";
            NotificationController.showAlert("Lỗi xác thực", errorMsg);
        }
    }


    private void showSignUp() {
        VBox signUpDialog = createSignUpDialog();
        // Tráo đổi vị trí: SignUp bên trái, Door bên phải
        LoginEffectHelper.playSwitchAnimation(door, loginDialog, () -> {
            rootHBox.getChildren().setAll(signUpDialog, door);
        }, rootHBox);

    }

    private VBox createSignUpDialog() {
        VBox signUp = new VBox(20); // Spacing giữa các thành phần
        signUp.setPrefWidth(420.0);
        signUp.setAlignment(Pos.TOP_LEFT);
        signUp.setStyle("-fx-background-color: #f7f9fb; -fx-padding: 60px 50px 40px 50px;");
        HBox.setHgrow(signUp, Priority.ALWAYS);

        // --- Tiêu đề ---
        VBox header = new VBox(5);
        Text title = new Text("Create Account");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 800; -fx-fill: #1A1A1A;");
        Text subTitle = new Text("Register to start your journey.");
        subTitle.setStyle("-fx-fill: #45464d; -fx-font-size: 14px;");
        header.getChildren().addAll(title, subTitle);

        // --- 1. Username Field ---
        TextField usernameField = createStyledTextField("Username", "✉");

        // --- 2. Email Field ---
        TextField emailField = createStyledTextField("Email address", "@");

        // --- 3. Password Field (Đã tích hợp chức năng Ẩn/Hiện) ---
        StackPane passwordContainer = new StackPane();

        // Trường nhập mật khẩu (ẩn)
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        passwordField.setStyle("-fx-background-color: white; -fx-border-color: #c6c6cd; -fx-border-radius: 10px; -fx-padding: 12px; -fx-font-size: 14px;");

        // Trường hiển thị mật khẩu (hiện)
        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("••••••••");
        visiblePasswordField.setStyle("-fx-background-color: white; -fx-border-color: #c6c6cd; -fx-border-radius: 10px; -fx-padding: 12px; -fx-font-size: 14px;");
        visiblePasswordField.setVisible(false); // Mặc định ẩn

        // Đồng bộ dữ liệu
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        // Icon con mắt
        Text toggleSignUpPasswordIcon = new Text("👁");
        toggleSignUpPasswordIcon.setStyle("-fx-cursor: hand; -fx-font-size: 16px;");
        StackPane.setAlignment(toggleSignUpPasswordIcon, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggleSignUpPasswordIcon, new javafx.geometry.Insets(0, 15, 0, 0));

        final boolean[] isSignUpPasswordVisible = {false};
        toggleSignUpPasswordIcon.setOnMouseClicked(e -> {
            isSignUpPasswordVisible[0] = !isSignUpPasswordVisible[0];
            if (isSignUpPasswordVisible[0]) {
                toggleSignUpPasswordIcon.setText("🙈");
                visiblePasswordField.setVisible(true);
                passwordField.setVisible(false);
            } else {
                toggleSignUpPasswordIcon.setText("👁");
                visiblePasswordField.setVisible(false);
                passwordField.setVisible(true);
            }
        });

        passwordContainer.getChildren().addAll(visiblePasswordField, passwordField, toggleSignUpPasswordIcon);

        // --- 4. Role Selection (ComboBox) ---
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.setItems(FXCollections.observableArrayList("Bidder", "Seller"));
        roleCombo.setPromptText("Select your role");
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setStyle("-fx-background-color: white; -fx-border-color: #c6c6cd; -fx-border-radius: 10px; -fx-padding: 5px; -fx-font-size: 14px;");

        // --- Nút Đăng ký ---
        Button btnSignUp = new Button("Create Account");
        btnSignUp.setMaxWidth(Double.MAX_VALUE);
        btnSignUp.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 15px; -fx-background-radius: 8px; -fx-font-size: 16px; -fx-cursor: hand;");
        btnSignUp.setOnAction(event -> {
            String userName = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String role = roleCombo.getValue();

            if (userName.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
                NotificationController.showAlert("Đăng ký không hợp lệ", "Vui lòng điền đầy đủ toàn bộ các trường thông tin đăng ký!");
                return;
            }

            RegisterPayload signUpPayload = new RegisterPayload();
            signUpPayload.username = userName;
            signUpPayload.password = password;
            signUpPayload.email = email;
            signUpPayload.role = role.toUpperCase();

            Response response = ServerConnection.getInstance().send(RequestType.REGISTER, signUpPayload);

            if (response != null && response.isSuccess()) {
                NotificationController.showNotification("Đăng ký thành công", "Tài khoản của bạn đã được khởi tạo thành công trên hệ thống!");

                System.out.println("Đăng ký thành công!");

                LoginEffectHelper.playSwitchAnimation(signUp, door, () -> {
                    rootHBox.getChildren().setAll(door, loginDialog);
                }, rootHBox);

            } else {
                // Hiển thị thông báo lỗi từ Server trả về (Ví dụ: "Tên tài khoản hoặc email đã tồn tại")
                String errorMsg = (response != null) ? response.getMessage() : "Không thể kết nối đến máy chủ!";
                NotificationController.showAlert("Đăng ký thất bại", errorMsg);
            }

        });

        // --- Nút Quay lại Login ---
        HBox footer = new HBox(5);
        footer.setAlignment(Pos.CENTER);
        Label lab1 = new Label("Already have an account?");
        Label labLink = new Label("Login");
        labLink.setStyle("-fx-text-fill: #4169E1; -fx-font-weight: bold; -fx-cursor: hand;");
        labLink.setOnMouseClicked(e -> {
            LoginEffectHelper.playSwitchAnimation(signUp, door, () -> {
                rootHBox.getChildren().setAll(door, loginDialog);
            }, rootHBox);
        });

        footer.getChildren().addAll(lab1, labLink);

        // Thêm tất cả vào SignUp VBox
        signUp.getChildren().addAll(
                header,
                new Label("Username"), usernameField,
                new Label("Email"), emailField,
                new Label("Password"), passwordContainer,
                new Label("Role"), roleCombo,
                btnSignUp, footer
        );

        return signUp;
    }

    // Hàm phụ để tạo TextField có style nhanh
    private TextField createStyledTextField(String prompt, String icon) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: white; -fx-border-color: #c6c6cd; -fx-border-radius: 10px; -fx-padding: 12px; -fx-font-size: 14px;");
        return tf;
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            // Lấy vị trí con trỏ hiện tại của trường Ẩn để chuyển sang trường Hiện
            int caretPosition = enterPassword.getCaretPosition();
            // Hiển thị mật khẩu
            togglePasswordIcon.setText("🙈"); // Đổi icon sang nhắm mắt
            visiblePassword.setVisible(true);
            enterPassword.setVisible(false);
            // Đưa con trỏ chuột về cuối dòng văn bản
            visiblePassword.requestFocus();
            visiblePassword.positionCaret(caretPosition);
        } else {
            // Lấy vị trí con trỏ hiện tại của trường Ẩn để chuyển sang trường Hiện
            int caretPosition = enterPassword.getCaretPosition();
            // Ẩn mật khẩu
            togglePasswordIcon.setText("👁"); // Đổi icon sang mở mắt
            visiblePassword.setVisible(false);
            enterPassword.setVisible(true);
            // Đưa con trỏ chuột về cuối dòng văn bản
            visiblePassword.requestFocus();
            visiblePassword.positionCaret(caretPosition);
        }
    }

    // Quên mật khẩu
    @FXML
    private void hanleClickForgotPassword(ActionEvent event) {
        FXMLLoader loader = GenerationSupport.changeScene(event, "forgotPassword-view.fxml", "Quên mật khẩu!");

        if (loader != null) {
            ForgotPasswordController controller = loader.getController();
        }

    }

    //Phương thức xử lý khi nhấn vào Điều khoản dịch vụ
    @FXML
    void handleOpenTerms(ActionEvent event) {
        LoginEffectHelper.openFile("documents/terms.pdf");
    }

    //Phương thức xử lý khi nhấn vào Chính sách bảo mật
    @FXML
    void handleOpenPrivacy(ActionEvent event) {
        LoginEffectHelper.openFile("documents/privacy.pdf");
    }
}

