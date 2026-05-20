package com.auction.client;

import com.auction.network.Notification;
import com.auction.network.Request;
import com.auction.network.RequestType;
import com.auction.network.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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

            // Gửi request
            out.writeObject(
                    new Request(type, payload)
            );

            out.flush();

            out.reset();

            // Đọc dữ liệu trả về
            while (true) {

                Object obj = in.readObject();

                if (obj instanceof Response r)
                    return r;

                if (obj instanceof Notification n
                        && notificationHandler != null) {

                    final Notification notif = n;

                    // Chạy trên JavaFX thread
                    javafx.application.Platform.runLater(

                            () -> notificationHandler.accept(notif)
                    );
                }
            }

        } catch (IOException | ClassNotFoundException e) {

            // Báo lỗi kết nối
            System.err.println(
                    "Lỗi gửi request: "
                            + e.getMessage()
            );

            return Response.error(
                    "Mất kết nối tới server"
            );
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
                    }

                } catch (IOException | ClassNotFoundException e) {
                    // Kiểm tra xem lỗi xảy ra do ta chủ động đóng kết nối/dừng luồng hay lỗi mạng thật
                    if (Thread.currentThread().isInterrupted() || socket == null || socket.isClosed()) {
                        System.out.println("Luồng lắng nghe đã kết thúc an toàn (Đăng xuất/Ngắt kết nối chủ động).");
                    } else {
                        System.err.println("Listener lỗi kết nối đột ngột: " + e.getMessage());
                    }
                    break; // Thoát khỏi vòng lặp, kết thúc Thread
                }
            }
        });

        // Thread chạy nền
        listenerThread.setDaemon(true);

        // Start thread
        listenerThread.start();
    }

    public void stopListening() {
        System.out.println("Đang dừng luồng lắng nghe (listenerThread)...");

        if (listenerThread != null && listenerThread.isAlive()) {
            // Gửi tín hiệu ngắt (interrupt) tới thread
            listenerThread.interrupt();
        }

        // Hủy bỏ handler để tránh rò rỉ bộ nhớ hoặc xử lý nhầm dữ liệu cũ
        this.notificationHandler = null;
        System.out.println("Đã dừng luồng lắng nghe thành công.");
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
