package com.example.group11;

import com.auction.client.ServerConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Kết nối tới server TRƯỚC khi mở bất kỳ màn hình nào
        try {
            ServerConnection.getInstance().connect();
        } catch (IOException e) {
            // Server chưa chạy — hiện thông báo lỗi
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Không thể kết nối tới server!\nHãy chạy AuctionServer trước.", ButtonType.OK);
            alert.showAndWait();
            return; // thoát app
        }

        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 750);
        stage.setTitle("Welcome to Auction Floor!");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            try {
                ServerConnection.getInstance().disconnect();
            } catch (IOException ignored) {
            }
        });
        stage.show();
    }
}
//        FXMLLoader fxmlLoader2 = new FXMLLoader(MainApplication.class.getResource(""));
//        Scene scene2 = new Scene(fxmlLoader2.load(), 1100, 750);
//        stage.setTitle("Auction List");
//        stage.setScene(scene2);
//        stage.show();

//        FXMLLoader fxmlLoader3 = new FXMLLoader(MainApplication.class.getResource("item-view.fxml"));
//        Scene scene3 = new Scene(fxmlLoader3.load(), 1100, 750);
//        stage.setTitle("Item");
//        stage.setScene(scene3);
//        stage.show();

//        FXMLLoader fxmlLoader4 = new FXMLLoader(MainApplication.class.getResource("forgotPassword-view.fxml"));
//        Scene scene4 = new Scene(fxmlLoader4.load(), 1100, 750);
//        stage.setTitle("Forgot password");
//        stage.setScene(scene4);
//        stage.show();

//        FXMLLoader fxmlLoader5 = new FXMLLoader(MainApplication.class.getResource("bidderAuctionList-view.fxml"));
//        Scene scene5 = new Scene(fxmlLoader5.load(), 1100, 750);
//        stage.setTitle("Auction floor of Bidder");
//        stage.setScene(scene5);
//        stage.show();
//
//        FXMLLoader fxmlLoader6 = new FXMLLoader(MainApplication.class.getResource("sellerAuctionList-view.fxml"));
//        Scene scene6 = new Scene(fxmlLoader6.load(), 1100, 750);
//        stage.setTitle("Auction floor of Seller");
//        stage.setScene(scene6);
//        stage.show();

//        FXMLLoader fxmlLoader7 = new FXMLLoader(MainApplication.class.getResource("liveAuction-view.fxml"));
//        Scene scene7 = new Scene(fxmlLoader7.load(), 1100, 750);
//        stage.setTitle("Live Auction");
//        stage.setScene(scene7);
//        stage.show();
//    }
}
