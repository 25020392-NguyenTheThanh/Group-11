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
            "DB_URL", "jdbc:mysql://sql12.freesqldatabase.com:3306/sql12828332"
                    + "?useUnicode=true"
                    + "&characterEncoding=utf8"
                    + "&serverTimezone=Asia/Ho_Chi_Minh"
                    + "&useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
    );
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "sql12828332");
    private static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "lBL6xIvN17");

//    private static final String DB_URL = System.getenv().getOrDefault(
//            "DB_URL",
//            "jdbc:mysql://localhost:3306/auction_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh");
//    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
//    private static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "ngtien123@@@");

    private static DatabaseConnection instance;
    private static final java.util.List<Connection> connectionPool = new java.util.ArrayList<>();
    private static final int POOL_SIZE = 10;

    private DatabaseConnection() {}

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // Trả về một kết nối đại diện (proxy). Khi caller đóng proxy này, kết nối vật lý thực tế được trả về pool để tái sử dụng.
    public static Connection getConnection() throws SQLException {
        if (instance == null) {
            getInstance();
        }
        Connection physicalConn = null;
        synchronized (connectionPool) {
            while (!connectionPool.isEmpty()) {
                Connection conn = connectionPool.remove(connectionPool.size() - 1);
                try {
                    if (conn != null && !conn.isClosed() && conn.isValid(1)) {
                        physicalConn = conn;
                        break;
                    }
                } catch (SQLException e) {
                    try { conn.close(); } catch (SQLException ignored) {}
                }
            }
        }
        if (physicalConn == null) {
            physicalConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }
        
        final Connection finalPhysicalConn = physicalConn;
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
            DatabaseConnection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            new java.lang.reflect.InvocationHandler() {
                @Override
                public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("close")) {
                        synchronized (connectionPool) {
                            try {
                                if (finalPhysicalConn != null && !finalPhysicalConn.isClosed() && finalPhysicalConn.isValid(1) && connectionPool.size() < POOL_SIZE) {
                                    connectionPool.add(finalPhysicalConn);
                                    return null;
                                }
                            } catch (SQLException ignored) {}
                        }
                        if (finalPhysicalConn != null) {
                            finalPhysicalConn.close();
                        }
                        return null;
                    }
                    try {
                        return method.invoke(finalPhysicalConn, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            }
        );
    }

}