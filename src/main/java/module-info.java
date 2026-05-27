module com.example.group11 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires jdk.jfr;
    requires java.sql;
    // jdk.jsobject;

    opens com.example.group11 to javafx.fxml;
    exports com.example.group11;
    exports com.example.group11.controller;
    exports com.auction.model.item;
    exports com.auction.model.auction;
    exports com.auction.model.user;
    exports com.auction.exception;
    exports com.auction.pattern.factory;
    exports com.auction.data;
    exports com.auction.manager;
    opens com.example.group11.controller to javafx.fxml;
    opens com.auction.model.user to javafx.base, javafx.fxml;
    opens com.auction.model.item to javafx.base, javafx.fxml;
    opens com.auction.model.auction to javafx.base, javafx.fxml;
    opens com.auction.model.entity to javafx.base, javafx.fxml;
}