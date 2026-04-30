package com.example.group11;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader1 = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
        Scene scene1 = new Scene(fxmlLoader1.load(), 1100, 750);
        stage.setTitle("Welcome to auction floor!");
        stage.setScene(scene1);
        stage.show();




//        FXMLLoader fxmlLoader3 = new FXMLLoader(MainApplication.class.getResource("auctionList-view.fxml"));
//        Scene scene3 = new Scene(fxmlLoader3.load(), 1100, 750);
//        stage.setTitle("Auction List");
//        stage.setScene(scene3);
//        stage.show();
    }
}
