package com.auction.data;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.io.InputStream;
import java.util.Properties;

/**
 * Quản lý kết nối tới database.
 * Singleton — chỉ giữ cấu hình, mỗi lần gọi getConnection() mở một kết nối mới.
 */

public class DatabaseConnection {
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASS;

    // Khối static này sẽ chạy ngay khi class được gọi lần đầu tiên
    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("config.properties")) {

            Properties prop = new Properties();

            if (input == null) {
                System.err.println("Lỗi: Không tìm thấy file config.properties trong thư mục resources!");
                // ném ra Exception ở đây để server dừng lại nếu không có cấu hình
            } else {
                // Tải các thuộc tính từ file
                prop.load(input);

                // Gán giá trị vào các biến
                DB_URL = prop.getProperty("db.url");
                DB_USER = prop.getProperty("db.user");
                DB_PASS = prop.getProperty("db.pass");

                System.out.println("Đã tải cấu hình Database thành công.");
            }
        } catch (Exception ex) {
            System.err.println("Lỗi khi đọc file config.properties:");
            ex.printStackTrace();
        }
    }
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
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            }
        );
    }

}