package com.auction.server;

import com.auction.manager.AuctionManager;
import com.auction.manager.ItemManager;
import com.auction.manager.UserManager;
import com.auction.network.Notification;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.auction.data.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class AuctionServer {
    private static final int PORT = 9999 ;
    // Danh sách tất cả client đang kết nối — dùng CopyOnWriteArrayList
    // vì nhiều thread đọc/ghi đồng thời
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public void start() {
        // Cập nhật Database Schema để hỗ trợ trạng thái UNSOLD
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE items MODIFY COLUMN status ENUM('AVAILABLE','IN_AUCTION','SOLD','UNSOLD') NOT NULL DEFAULT 'AVAILABLE'");
            System.out.println("[DATABASE] Đã đồng bộ cấu trúc bảng items (thêm trạng thái UNSOLD nếu chưa có).");
        } catch (SQLException e) {
            System.err.println("[DATABASE] Không thể thay đổi cấu trúc bảng items (có thể đã có hoặc lỗi quyền): " + e.getMessage());
        }

        // Load auctions from database at server startup
        AuctionManager.getInstance().loadAuctionsFromDatabase();

        new AuctionTimer(this).start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                connectedClients.add(handler);
                threadPool.submit(handler);
                System.out.println("Client mới kết nối : " + clientSocket.getInetAddress());
            }
        } catch (IOException e) {
            System.err.println("Server lỗi : " + e.getMessage());

        }
    }
    // gọi khi có bid mới , thông báo cho tất cả các client
    public void broadcast(Notification notification){
        for (ClientHandler handler : connectedClients){
            handler.sendNotification(notification);
        }
    }
    // xóa các handler đã chết
    public void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
    }

    public List<ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}
