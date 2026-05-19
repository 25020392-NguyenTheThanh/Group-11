package com.example.group11.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import java.io.IOException;

public class GenerationSupport {

    public static FXMLLoader changeScene(ActionEvent event, String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(LoginEffectHelper.class.getResource("/com/example/group11/" + fxmlFile));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);

            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();

            return loader; // Trả về loader để lấy controller bên ngoài
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static FXMLLoader changeScene(javafx.scene.Node node, String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(GenerationSupport.class.getResource("/com/example/group11/" + fxmlFile));
            Parent root = loader.load();

            // Lấy Stage trực tiếp từ linh kiện (Node) đang hiển thị một cách an toàn
            Stage stage = null;
            if (node != null && node.getScene() != null) {
                stage = (Stage) node.getScene().getWindow();
            }

            // Nếu vẫn không lấy được (trường hợp cực kỳ hiếm), lấy cửa sổ đầu tiên đang hiển thị của ứng dụng
            if (stage == null) {
                stage = (Stage) Stage.getWindows().stream()
                        .filter(javafx.stage.Window::isShowing)
                        .findFirst()
                        .orElse(null);
            }

            if (stage == null) {
                throw new NullPointerException("Không tìm thấy cửa sổ (Stage) hợp lệ để chuyển cảnh!");
            }

            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();

            return loader;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Tự động cập nhật nhãn (Text) của MenuButton khi người dùng chọn một Item bên trong
    public static void setupMenuButtonUpdate(MenuButton menuButton) {
        for (MenuItem item : menuButton.getItems()) {
            item.setOnAction(event -> {
                menuButton.setText(item.getText().toUpperCase());
                // Khu vực xử lý logic lọc dữ liệu hoặc cập nhật trạng thái tương ứng
                System.out.println("Seller/Bidder đang chọn: " + item.getText());
            });
        }
    }


}
