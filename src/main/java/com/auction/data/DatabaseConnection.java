package com.auction.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Quản lý kết nối tới database.
 * Singleton — chỉ giữ cấu hình, mỗi lần gọi getConnection() mở một kết nối mới.
 */
public class DatabaseConnection {

    private static final String DB_URL = System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:mysql://localhost:3306/auction_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "tuan1301");

    private static DatabaseConnection instance;

    private DatabaseConnection() {}

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // Mở và trả về một kết nối mới tới database. Caller chịu trách nhiệm đóng kết nối.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

}