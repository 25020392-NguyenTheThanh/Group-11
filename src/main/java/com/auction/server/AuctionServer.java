package com.auction.server;

import com.auction.manager.AuctionManager;
import com.auction.manager.UserManager;
import com.auction.network.Notification;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
    private static final int PORT = 9999 ;
    // Danh sách tất cả client đang kết nối — dùng CopyOnWriteArrayList
    // vì nhiều thread đọc/ghi đồng thời
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public void start() {
        // Load dữ liệu từ file khi server khởi động
        AuctionManager.getInstance().loadFromDisk();
        UserManager.getInstance().loadFromDisk();

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

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}
