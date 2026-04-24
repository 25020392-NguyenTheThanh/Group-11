package com.example.group11;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader1 = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene1 = new Scene(fxmlLoader1.load(), 320, 240);
        stage.setTitle("Welcome to auction floor!");
        stage.setScene(scene1);
        stage.show();

//        FXMLLoader fxmlLoader2 = new FXMLLoader(HelloApplication.class.getResource("createAccount-view.fxml"));
//
//        Scene scene2 = new Scene(fxmlLoader2.load(), 1100, 750);
//
//        stage.setTitle("AuctionPro - Create Account");
//        stage.setScene(scene2);
//
//
//        stage.setResizable(true);
//
//        stage.show();
    }
}
