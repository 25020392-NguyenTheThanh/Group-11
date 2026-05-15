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
