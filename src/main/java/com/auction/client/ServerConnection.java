package com.auction.client;

import com.auction.network.Notification;
import com.auction.network.Request;
import com.auction.network.RequestType;
import com.auction.network.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

// Class quản lý kết nối client tới server
public class ServerConnection {

    // Địa chỉ server
    private static final String HOST = "localhost";

    // Port server
    private static final int PORT = 9999;

    // Singleton instance
    // volatile giúp đồng bộ thread an toàn
    private static volatile ServerConnection instance;

    // Socket kết nối tới server
    private Socket socket;

    // Luồng ghi dữ liệu tới server
    private ObjectOutputStream out;

    // Luồng đọc dữ liệu từ server
    private ObjectInputStream in;

    // Thread lắng nghe notification từ server
    private Thread listenerThread;

    // Hàm xử lý notification
    private Consumer<Notification> notificationHandler;

    // HÀNG ĐỢI: Dùng để tuồn Response từ listenerThread sang hàm send()
    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();

    // Constructor private để dùng Singleton
    private ServerConnection() {}

    // Lấy instance duy nhất
    public static ServerConnection getInstance() {

        // Nếu chưa có instance
        if (instance == null) {

            synchronized (ServerConnection.class) {

                // Kiểm tra lại để tránh tạo nhiều object
                if (instance == null) {

                    instance = new ServerConnection();
                }
            }
        }

        return instance;
    }

    // Kết nối tới server
    public void connect() throws IOException {

        // Tạo socket
        socket = new Socket(HOST, PORT);

        // Tạo output stream trước
        // tránh deadlock
        out = new ObjectOutputStream(
                socket.getOutputStream()
        );

        out.flush();

        // Tạo input stream
        in = new ObjectInputStream(
                socket.getInputStream()
        );

        System.out.println("Đã kết nối tới server");
    }

    // Gửi request lên server
    // synchronized để tránh nhiều thread đọc ghi cùng lúc
    public synchronized Response send(
            RequestType type,
            Object payload
    ) {

        try {
            // 1. Chỉ đồng bộ việc ghi dữ liệu để các thread không tranh nhau ghi
            synchronized (out) {
                out.writeObject(new Request(type, payload));
                out.flush();
                out.reset();
            }

            // 2. Đọc dữ liệu trả về
            if (listenerThread != null && listenerThread.isAlive()) {
                // KỊCH BẢN 1: Đã login, listener đang chạy.
                // Ta đứng đợi ở Queue chờ Listener nhặt Response quăng vào.
                return responseQueue.take();
            } else {
                // KỊCH BẢN 2: Chưa login, listener chưa chạy.
                // Hàm send tự mình đọc trực tiếp.
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof Response r) return r;

                    if (obj instanceof Notification n && notificationHandler != null) {
                        javafx.application.Platform.runLater(() -> notificationHandler.accept(n));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi request: " + e.getMessage());
            return Response.error("Mất kết nối tới server");
        }
    }

    // Bắt đầu thread lắng nghe notification
    public void startListening() {

        if (listenerThread != null
                && listenerThread.isAlive())
            return;

        // Tạo thread listener
        listenerThread = new Thread(() -> {

            // Chạy khi socket còn mở
            while (socket != null
                    && !socket.isClosed()) {

                try {
                    Object obj;
                    synchronized (this) {
                        obj = in.readObject();
                    }

                    // Nếu nhận notification
                    if (obj instanceof Notification n
                            && notificationHandler != null) {

                        // Chạy trên JavaFX thread
                        javafx.application.Platform.runLater(

                                () -> notificationHandler.accept(n)
                        );
                    }// Nếu nhặt được Response thì ném vào queue
                    else if (obj instanceof Response r) {
                        responseQueue.offer(r);
                    }

                } catch (IOException | ClassNotFoundException e) {

                    // Nếu socket chưa đóng thì báo lỗi
                    if (socket != null
                            && !socket.isClosed()) {

                        System.err.println(
                                "Listener lỗi: "
                                        + e.getMessage()
                        );
                    }

                    break;
                }
            }
        });

        // Thread chạy nền
        listenerThread.setDaemon(true);

        // Start thread
        listenerThread.start();
    }

    // Gán hàm xử lý notification
    public void setNotificationHandler(
            Consumer<Notification> handler
    ) {

        this.notificationHandler = handler;
    }

    // Kiểm tra trạng thái kết nối
    public boolean isConnected() {

        return socket != null
                && socket.isConnected()
                && !socket.isClosed();
    }

    // Ngắt kết nối server
    public void disconnect() throws IOException {

        if (socket != null)
            socket.close();
    }
}