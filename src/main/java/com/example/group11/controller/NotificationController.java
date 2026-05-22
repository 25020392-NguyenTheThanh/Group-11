package com.example.group11.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import java.util.Optional;

/**
 * Bộ điều khiển hiển thị thông báo (Notification Controller) của ứng dụng.
 * Quản lý việc hiển thị các hộp thoại thông báo, cảnh báo, xác nhận và báo lỗi
 * đồng bộ với giao diện tối (Dark Theme) của hệ thống.
 */
public class NotificationController {

    /**
     * Hiển thị thông báo thành công hoặc thông tin hệ thống (Information Dialog).
     *
     * @param title Tiêu đề của thông báo
     * @param content Nội dung chi tiết của thông báo
     */
    public static void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("HANK AUCTIONS");
        alert.setHeaderText("🎉 " + title); // Thêm emoji đồng bộ với hệ thống thông báo sidebar
        alert.setContentText(content);

        applyDarkTheme(alert);
        alert.showAndWait();
    }

    /**
     * Hiển thị thông báo cảnh báo hoặc lỗi thao tác từ người dùng (Warning Dialog).
     *
     * @param title Tiêu đề của cảnh báo
     * @param content Nội dung chi tiết cảnh báo
     */
    public static void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("HANK AUCTIONS");
        alert.setHeaderText("⚠️ " + title);
        alert.setContentText(content);

        applyDarkTheme(alert);
        alert.showAndWait();
    }

    /**
     * Hiển thị hộp thoại xác nhận hành động với hai lựa chọn Có/Không (Confirmation Dialog).
     *
     * @param title Tiêu đề của hộp thoại
     * @param headerText Tiêu đề phụ (Header)
     * @param contentText Nội dung cần người dùng xác nhận
     * @param yesButtonText Nhãn nút đồng ý/xác nhận
     * @param noButtonText Nhãn nút hủy/bỏ qua
     * @return true nếu người dùng chọn nút đồng ý, ngược lại trả về false
     */
    public static boolean showConfirmation(String title, String headerText, String contentText, String yesButtonText, String noButtonText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("HANK AUCTIONS");
        alert.setHeaderText("❓ " + headerText);
        alert.setContentText(contentText);

        ButtonType buttonYes = new ButtonType(yesButtonText);
        ButtonType buttonNo = new ButtonType(noButtonText);
        alert.getButtonTypes().setAll(buttonYes, buttonNo);

        applyDarkTheme(alert);

        // Tạo điểm nhấn nổi bật riêng cho nút Xác nhận/Đồng ý bằng màu vàng rực
        Button yesBtn = (Button) alert.getDialogPane().lookupButton(buttonYes);
        if (yesBtn != null) {
            yesBtn.setStyle("-fx-background-color: #ffd700; -fx-text-fill: #3a3000; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-weight: bold;");
        }

        Optional<ButtonType> response = alert.showAndWait();
        return response.isPresent() && response.get() == buttonYes;
    }

    /**
     * Hiển thị thông báo lỗi nghiêm trọng của hệ thống hoặc lỗi thực thi (Error Dialog).
     *
     * @param title Tiêu đề của lỗi
     * @param content Nội dung chi tiết lỗi
     */
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
                    "    -fx-background-color: #1A1A1A;" +
                    "    -fx-border-color: #ffd700;" +
                    "    -fx-border-width: 1px;" +
                    "    -fx-border-radius: 12px;" +
                    "    -fx-background-radius: 12px;" +
                    "}" +
                    ".dialog-pane .label {" +
                    "    -fx-text-fill: #d0c6ab;" + // Màu chữ xám bạc sang trọng
                    "    -fx-font-size: 13px;" +
                    "}" +
                    ".dialog-pane:header .header-panel {" +
                    "    -fx-background-color: #0A0A0A;" + // Nền header đậm hơn giống card panel
                    "    -fx-padding: 15px;" +
                    "    -fx-border-color: transparent transparent #262626 transparent;" +
                    "    -fx-border-width: 1px;" +
                    "}" +
                    ".dialog-pane:header .header-panel .label {" +
                    "    -fx-text-fill: #FFD700;" + // Chữ tiêu đề màu vàng Gold đặc trưng
                    "    -fx-font-size: 15px;" +
                    "    -fx-font-weight: bold;" +
                    "}" +
                    ".dialog-pane .button {" +
                    "    -fx-background-color: #262626;" +
                    "    -fx-text-fill: white;" +
                    "    -fx-background-radius: 4px;" +
                    "    -fx-font-weight: bold;" +
                    "    -fx-cursor: hand;" +
                    "    -fx-padding: 6px 16px;" +
                    "}" +
                    ".dialog-pane .button:hover {" +
                    "    -fx-background-color: #ffd700;" + // Hiệu ứng sáng nhẹ khi di chuột qua
                    "    -fx-text-fill: #3a3000;" +
                    "}" +
                    ".dialog-pane .button:focused {" +
                    "    -fx-border-color: #FFD700;" +
                    "    -fx-border-radius: 4px;" +
                    "}" +
                    ".dialog-pane .text-field {" +
                    "    -fx-background-color: #0A0A0A;" +
                    "    -fx-text-fill: #ffd700;" +
                    "    -fx-border-color: #262626;" +
                    "    -fx-border-width: 1px;" +
                    "    -fx-border-radius: 4px;" +
                    "    -fx-background-radius: 4px;" +
                    "    -fx-padding: 8px;" +
                    "    -fx-font-weight: bold;" +
                    "    -fx-font-size: 14px;" +
                    "}" +
                    ".dialog-pane .text-field:focused {" +
                    "    -fx-border-color: #ffd700;" +
                    "}"
     ).replace(" ", "%20"); // Mã hóa khoảng trắng để đảm bảo tính hợp lệ của URI CSS

    /**
     * Hàm hỗ trợ áp dụng giao diện tối (Dark Theme) cho hộp thoại.
     * Loại bỏ các biểu tượng mặc định có khung trắng lỗi thời của hệ điều hành.
     *
     * @param dialog Đối tượng Dialog/Alert cần áp dụng giao diện tối
     */
    public static void applyDarkTheme(javafx.scene.control.Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(DARK_THEME_CSS);

        // Loại bỏ các biểu tượng mặc định có khung trắng lỗi thời của OS
        dialog.setGraphic(null);
    }

}
