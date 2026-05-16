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
    requires org.junit.jupiter.api;
    requires java.sql;
    // jdk.jsobject;

    opens com.example.group11 to javafx.fxml;
    exports com.example.group11;
    exports com.example.group11.controller;
    opens com.example.group11.controller to javafx.fxml;

}