package com.example.group11;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
//        FXMLLoader fxmlLoader1 = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
//        Scene scene1 = new Scene(fxmlLoader1.load(), 1100, 750);
//        stage.setTitle("Welcome to auction floor!");
//        stage.setScene(scene1);
//        stage.show();


//        FXMLLoader fxmlLoader2 = new FXMLLoader(MainApplication.class.getResource("registerProduct-view.fxml"));
//        Scene scene2 = new Scene(fxmlLoader2.load(), 1100, 750);
//        stage.setTitle("Register Product");
//        stage.setScene(scene2);
//        stage.show();

//        FXMLLoader fxmlLoader3 = new FXMLLoader(MainApplication.class.getResource("itemBidder-view.fxml"));
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

        FXMLLoader fxmlLoader7 = new FXMLLoader(MainApplication.class.getResource("liveAuction-view.fxml"));
        Scene scene7 = new Scene(fxmlLoader7.load(), 1100, 750);
        stage.setTitle("Live Auction");
        stage.setScene(scene7);
        stage.show();
    }
}
