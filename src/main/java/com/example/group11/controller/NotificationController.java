package com.example.group11.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import java.util.Optional;

public class NotificationController {

    // Hiển thị thông báo thành công / thông tin hệ thống (Information)
    public static void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("HANK AUCTIONS");
        alert.setHeaderText("🎉 " + title); // Thêm emoji đồng bộ với hệ thống thông báo sidebar
        alert.setContentText(content);

        applyDarkTheme(alert);
        alert.showAndWait();
    }

    // Hiển thị thông báo cảnh báo / lỗi thao tác (Warning)
    public static void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("HANK AUCTIONS");
        alert.setHeaderText("⚠️ " + title);
        alert.setContentText(content);

        applyDarkTheme(alert);
        alert.showAndWait();
    }

    // Hiển thị hộp thoại xác nhận hành động (Confirmation)
    public static boolean showConfirmation(String title, String headerText, String contentText, String yesButtonText, String noButtonText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("HANK AUCTIONS");
        alert.setHeaderText("❓ " + headerText);
        alert.setContentText(contentText);

        ButtonType buttonYes = new ButtonType(yesButtonText);
        ButtonType buttonNo = new ButtonType(noButtonText);
        alert.getButtonTypes().setAll(buttonYes, buttonNo);

        applyDarkTheme(alert);

        // Tạo điểm nhấn nổi bật riêng cho nút Xác nhận/Đồng ý bằng viền vàng rực
        Button yesBtn = (Button) alert.getDialogPane().lookupButton(buttonYes);
        if (yesBtn != null) {
            yesBtn.setStyle("-fx-background-color: #112240; -fx-border-color: #FFD700; -fx-text-fill: #FFD700; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-weight: bold;");
        }

        Optional<ButtonType> response = alert.showAndWait();
        return response.isPresent() && response.get() == buttonYes;
    }
    // Hiển thị thông báo lỗi nghiêm trọng (Error)
    public static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("HANK AUCTIONS");
        alert.setHeaderText("❌ " + title); // Sử dụng emoji icon lỗi
        alert.setContentText(content);

        applyDarkTheme(alert);
        alert.showAndWait();
    }

    // Chuỗi CSS đồng bộ hoàn hảo với bộ màu mã nguồn HANK AUCTIONS
    private static final String DARK_THEME_CSS = "data:text/css," + (
            ".dialog-pane {" +
                    "    -fx-background-color: #0A192F;" +
                    "    -fx-border-color: #1E2D45;" +
                    "    -fx-border-width: 1.5px;" +
                    "    -fx-border-radius: 12px;" +
                    "    -fx-background-radius: 12px;" +
                    "}" +
                    ".dialog-pane .label {" +
                    "    -fx-text-fill: #94A3B8;" + // Màu chữ xám bạc sang trọng
                    "    -fx-font-size: 13px;" +
                    "}" +
                    ".dialog-pane:header .header-panel {" +
                    "    -fx-background-color: #112240;" + // Nền header đậm hơn giống card panel
                    "    -fx-padding: 15px;" +
                    "    -fx-border-color: transparent transparent #1E2D45 transparent;" +
                    "    -fx-border-width: 1px;" +
                    "}" +
                    ".dialog-pane:header .header-panel .label {" +
                    "    -fx-text-fill: #FFD700;" + // Chữ tiêu đề màu vàng Gold đặc trưng
                    "    -fx-font-size: 15px;" +
                    "    -fx-font-weight: bold;" +
                    "}" +
                    ".dialog-pane .button {" +
                    "    -fx-background-color: #1E2D45;" +
                    "    -fx-text-fill: white;" +
                    "    -fx-background-radius: 6px;" +
                    "    -fx-font-weight: bold;" +
                    "    -fx-cursor: hand;" +
                    "    -fx-padding: 6px 16px;" +
                    "}" +
                    ".dialog-pane .button:hover {" +
                    "    -fx-background-color: #25354F;" + // Hiệu ứng sáng nhẹ khi di chuột qua
                    "    -fx-text-fill: #FFD700;" +
                    "}" +
                    ".dialog-pane .button:focused {" +
                    "    -fx-border-color: #FFD700;" +
                    "    -fx-border-radius: 6px;" +
                    "}"
    ).replace(" ", "%20"); // Mã hóa khoảng trắng để đảm bảo tính hợp lệ của URI CSS

    /**
     * Hàm hỗ trợ áp dụng giao diện tối cho Alert
     */
    private static void applyDarkTheme(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(DARK_THEME_CSS);

        // Loại bỏ các biểu tượng mặc định có khung trắng lỗi thời của OS
        alert.setGraphic(null);
    }

}
