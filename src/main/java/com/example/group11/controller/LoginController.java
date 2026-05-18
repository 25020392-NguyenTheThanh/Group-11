package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.exception.AuthenticationException;
import com.auction.manager.UserManager;
import com.auction.model.user.User;
import com.auction.network.LoginPayload;
import com.auction.network.RequestType;
import com.auction.network.Response;
import javafx.collections.FXCollections;
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

    @FXML
    private Label errorLabel;

    private boolean isPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Thiết lập mô hình chuyển động.
        EquilibriumAnimation.startEquilibriumAnimation(scaleBar, boxContainer, coinContainer);

        // Thiết lập hiệu ứng gạch chân khi di chuột.
        EquilibriumAnimation.setupHoverEffect(termsLink);
        EquilibriumAnimation.setupHoverEffect(privacyLink);

        createAccountLabel.setOnMouseClicked(event -> showSignUp());

        userManager = UserManager.getInstance();

        visiblePassword.textProperty().bindBidirectional(enterPassword.textProperty());

    }

    @FXML
    void handleLogin(ActionEvent event) {
        String userName = username.getText();
        String passWord = enterPassword.getText();

        User loggedInUser = userManager.login(userName, passWord);
        // 1. Kiểm tra nếu để trống trường nhập liệu
        if (userName.isEmpty() || passWord.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            errorLabel.setVisible(true);
            return;
        }

        LoginPayload payload = new LoginPayload(userName , passWord);

        Response response = ServerConnection.getInstance().send(RequestType.LOGIN , payload);
        if (response.isSuccess()){
            ServerConnection.getInstance().startListening();
            User LoggedInuser = (User) response.getData();
            errorLabel.setVisible(false);
            FXMLLoader loader = switch (loggedInUser.getRole()) {
                case "BIDDER" -> EquilibriumAnimation.changeScene(event , "bidderAuctionList-view.fxml" , "Auction.floor");
                case "SELLER" -> EquilibriumAnimation.changeScene(event , "sellerAuctionList-view.fxml" , "Auction.floor");
                default -> null ;
            };

            if (loader != null){
                Object controller = loader.getController();
                if (controller instanceof BidderAuctionListController c) {
                    c.setUser(loggedInUser);
                } else if (controller instanceof SellerAuctionListController c){
                    c.setUser(loggedInUser);
                } else {
                    errorLabel.setText(response.getMessage());
                    errorLabel.setVisible(true);
                }
            }
        }
    }

    //Phương thức xử lý khi nhấn vào Điều khoản dịch vụ

    @FXML
    void handleOpenTerms(ActionEvent event) {
        EquilibriumAnimation.openFile("documents/terms.pdf");
    }

    //Phương thức xử lý khi nhấn vào Chính sách bảo mật

    @FXML
    void handleOpenPrivacy(ActionEvent event) {
        EquilibriumAnimation.openFile("documents/privacy.pdf");
    }

    private void showSignUp() {
        VBox signUpDialog = createSignUpDialog();
        // Tráo đổi vị trí: SignUp bên trái, Door bên phải
        EquilibriumAnimation.playSwitchAnimation(door, loginDialog, () -> {
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
        btnSignUp.onMouseClickedProperty().set(event -> {
            String userName = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String role = roleCombo.getValue();

            User user = userManager.register(userName, password, email, role);
            if (user != null) {
                EquilibriumAnimation.playSwitchAnimation(signUp, door, () -> {
                    rootHBox.getChildren().setAll(door, loginDialog);
                }, rootHBox);

            }

        });


        // --- Nút Quay lại Login ---
        HBox footer = new HBox(5);
        footer.setAlignment(Pos.CENTER);
        Label lab1 = new Label("Already have an account?");
        Label labLink = new Label("Login");
        labLink.setStyle("-fx-text-fill: #4169E1; -fx-font-weight: bold; -fx-cursor: hand;");
        labLink.setOnMouseClicked(e -> rootHBox.getChildren().setAll(door, loginDialog));
        labLink.setOnMouseClicked(e -> {
            EquilibriumAnimation.playSwitchAnimation(signUp, door, () -> {
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
            // Hiển thị mật khẩu
            togglePasswordIcon.setText("🙈"); // Đổi icon sang nhắm mắt
            visiblePassword.setVisible(true);
            enterPassword.setVisible(false);
        } else {
            // Ẩn mật khẩu
            togglePasswordIcon.setText("👁"); // Đổi icon sang mở mắt
            visiblePassword.setVisible(false);
            enterPassword.setVisible(true);
        }
    }

    // Quên mật khẩu
    @FXML
    private void hanleClickForgotPassword(ActionEvent event) {
        FXMLLoader loader = EquilibriumAnimation.changeScene(event, "forgotPassword-view.fxml", "Quên mật khẩu!");

        if (loader != null) {
            ForgotPasswordController controller = loader.getController();
        }

    }
}

