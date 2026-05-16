package com.auction.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL      = "jdbc:mysql://localhost:3306/auction_system"
            + "?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh"
            + "&allowPublicKeyRetrieval=true";
    // thêm username với password MySQL vào đây
    private static final String USERNAME = "";
    private static final String PASSWORD = "";

    private static Connection connection ;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL , USERNAME , PASSWORD);
        }
        return connection ;
    }
}
