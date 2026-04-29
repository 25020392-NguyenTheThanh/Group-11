package com.example.group11.controller;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
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
    private CheckBox rememberPassword;

    @FXML
    private Rectangle scaleBar;

    @FXML
    private Hyperlink termsLink;

    @FXML
    private TextField username;

    @FXML private HBox rootHBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        startEquilibriumAnimation();

        // Thiết lập hiệu ứng gạch chân khi di chuột (không cần file CSS)
        setupHoverEffect(termsLink);
        setupHoverEffect(privacyLink);

        createAccountLabel.setOnMouseClicked(event -> showSignUp());
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

    /**
     * Phương thức xử lý khi nhấn vào Điều khoản dịch vụ
     */
    @FXML
    void handleOpenTerms(ActionEvent event) {
        openFile("C:/Users/MY-PC/Downloads/Điều khoản dịch vụ.pdf");
    }

    /**
     * Phương thức xử lý khi nhấn vào Chính sách bảo mật
     */
    @FXML
    void handleOpenPrivacy(ActionEvent event) {
        openFile("C:/Users/MY-PC/Downloads/Chính sách bảo mật.pdf");
    }

    /**
     * Hàm dùng chung để mở tệp bằng ứng dụng mặc định của hệ thống
     */
    private void openFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    System.out.println("Desktop is not supported on this platform.");
                }
            } else {
                System.out.println("File không tồn tại: " + filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Hiệu ứng gạch chân khi di chuột (Hover)
     */
    private void setupHoverEffect(Hyperlink link) {
        link.setOnMouseEntered(e -> link.setUnderline(true));
        link.setOnMouseExited(e -> link.setUnderline(false));
    }

    private void playSwitchAnimation(Node leftNode, Node rightNode, Runnable onFinishedAction) {
        double width = rootHBox.getWidth() / 2; // Sử dụng một nửa chiều rộng thực tế của root
        Duration duration = Duration.seconds(0.6);

        // 1. Hiệu ứng di chuyển trượt
        TranslateTransition moveLeft = new TranslateTransition(duration, leftNode);
        moveLeft.setByX(width);
        moveLeft.setInterpolator(Interpolator.EASE_BOTH); // Gia tốc mượt ở hai đầu

        TranslateTransition moveRight = new TranslateTransition(duration, rightNode);
        moveRight.setByX(-width);
        moveRight.setInterpolator(Interpolator.EASE_BOTH);

        // 2. Hiệu ứng mờ dần (Fade) giúp giảm cảm giác khựng khi tráo đổi Node
        FadeTransition fadeOutLeft = new FadeTransition(duration.divide(1.5), leftNode);
        fadeOutLeft.setFromValue(1.0);
        fadeOutLeft.setToValue(0.5);

        FadeTransition fadeOutRight = new FadeTransition(duration.divide(1.5), rightNode);
        fadeOutRight.setFromValue(1.0);
        fadeOutRight.setToValue(0.5);

        ParallelTransition pt = new ParallelTransition(moveLeft, moveRight, fadeOutLeft, fadeOutRight);

        pt.setOnFinished(e -> {
            // Reset lại các thuộc tính trước khi tráo đổi
            leftNode.setTranslateX(0);
            rightNode.setTranslateX(0);
            leftNode.setOpacity(1.0);
            rightNode.setOpacity(1.0);

            // Thực hiện thay đổi cấu trúc layout
            onFinishedAction.run();

            // Thêm một hiệu ứng Fade In nhẹ cho nội dung mới xuất hiện
            Node newNode = rootHBox.getChildren().get(0); // Lấy node mới ở bên trái
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.3), newNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        pt.play();
    }

    private void showSignUp() {
        VBox signUpDialog = createSignUpDialog();
        // Tráo đổi vị trí: SignUp bên trái, Door bên phải
        playSwitchAnimation(door, loginDialog, () -> {
            rootHBox.getChildren().setAll(signUpDialog, door);
        });

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

        // --- 3. Password Field ---
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        passwordField.setStyle("-fx-background-color: white; -fx-border-color: #c6c6cd; -fx-border-radius: 10px; -fx-padding: 12px; -fx-font-size: 14px;");

        // --- 4. Role Selection (ComboBox) ---
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.setItems(FXCollections.observableArrayList("Admin", "Bidder", "Seller"));
        roleCombo.setPromptText("Select your role");
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        // Style cho ComboBox giống với TextField
        roleCombo.setStyle("-fx-background-color: white; -fx-border-color: #c6c6cd; -fx-border-radius: 10px; -fx-padding: 5px; -fx-font-size: 14px;");

        // --- Nút Đăng ký ---
        Button btnSignUp = new Button("Create Account");
        btnSignUp.setMaxWidth(Double.MAX_VALUE);
        btnSignUp.setStyle("-fx-background-color: #4169E1; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 15px; -fx-background-radius: 8px; -fx-font-size: 16px; -fx-cursor: hand;");

        // --- Nút Quay lại Login ---
        HBox footer = new HBox(5);
        footer.setAlignment(Pos.CENTER);
        Label lab1 = new Label("Already have an account?");
        Label labLink = new Label("Login");
        labLink.setStyle("-fx-text-fill: #4169E1; -fx-font-weight: bold; -fx-cursor: hand;");
        labLink.setOnMouseClicked(e -> rootHBox.getChildren().setAll(door, loginDialog));
        labLink.setOnMouseClicked(e -> {
            playSwitchAnimation(signUp, door, () -> {
                rootHBox.getChildren().setAll(door, loginDialog);
            });
        });

        footer.getChildren().addAll(lab1, labLink);

        // Thêm tất cả vào SignUp VBox
        signUp.getChildren().addAll(
                header,
                new Label("Username"), usernameField,
                new Label("Email"), emailField,
                new Label("Password"), passwordField,
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
}
