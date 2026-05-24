package com.auction.client;

import com.auction.network.*;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

    // Danh sách xử lý notification (Multicast)
    private final List<Consumer<Notification>> notificationHandlers = new CopyOnWriteArrayList<>();

    // Flag đánh dấu có request đang gửi đi và chờ response
    private boolean requestInProgress = false;

    // Buffer lưu trữ response nhận được từ listenerThread truyền qua cho send()
    private Response responseBuffer = null;

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
    public synchronized void connect() throws IOException {

        // Reset hoàn toàn trạng thái cũ trước khi kết nối mới
        listenerThread    = null;
        responseBuffer    = null;
        requestInProgress = false;

        // Tạo socket với timeout đọc 30 giây để tránh block vô hạn
        socket = new Socket(HOST, PORT);
        socket.setSoTimeout(30_000); // 30 s read timeout

        // Tạo output stream trước — tránh deadlock
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();

        // Tạo input stream
        in = new ObjectInputStream(socket.getInputStream());

        System.out.println("Đã kết nối tới server");
    }

    // Gửi request lên server
    public synchronized Response send(
            RequestType type,
            Object payload
    ) {
        // Chờ nếu có một request khác đang được xử lý
        while (requestInProgress) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Response.error("Yêu cầu bị ngắt quãng");
            }
        }

        requestInProgress = true;

        try {
            // Gửi request
            out.writeObject(
                    new Request(type, payload)
            );

            out.flush();

            out.reset();

            // Nếu listener thread đang chạy, ta đợi nó đọc và thông báo
            if (listenerThread != null && listenerThread.isAlive()) {
                long deadline = System.currentTimeMillis() + 30_000; // 30 s timeout
                while (responseBuffer == null) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        // Timeout: tránh block vô hạn nếu listener thread chết
                        return Response.error("Không nhận được phản hồi từ server (timeout)");
                    }
                    this.wait(remaining);
                }
                Response r = responseBuffer;
                responseBuffer = null; // consume
                return r;
            } else {
                // Nếu listener thread chưa chạy, ta tự đọc trực tiếp
                while (true) {
                    Object obj = in.readObject();

                    if (obj instanceof Response r)
                        return r;

                    if (obj instanceof Notification n) {
                        final Notification notif = n;
                        // Chạy trên JavaFX thread
                        Platform.runLater(() -> {
                            for (Consumer<Notification> handler : notificationHandlers) {
                                handler.accept(notif);
                            }
                        });
                    }
                }
            }

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            // Báo lỗi kết nối
            System.err.println(
                    "Lỗi gửi request: "
                            + e.getMessage()
            );

            return Response.error(
                    "Mất kết nối tới server"
            );
        } finally {
            requestInProgress = false;
            this.notifyAll(); // Đánh thức các thread khác đang chờ để gửi request tiếp theo
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
                    // Đọc mạng KHÔNG giữ lock đồng bộ
                    Object obj = in.readObject();

                    // Chỉ đồng bộ khi lưu responseBuffer và gọi handler
                    synchronized (this) {
                        if (obj instanceof Response r) {
                            responseBuffer = r;
                            this.notifyAll();
                        } else if (obj instanceof Notification n) {
                            final Notification notif = n;
                            // Chạy trên JavaFX thread
                            Platform.runLater(() -> {
                                for (Consumer<Notification> handler : notificationHandlers) {
                                    handler.accept(notif);
                                }
                            });
                        }
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
            listenerThread.interrupt();
        }
        // Đóng socket để unblock in.readObject() đang chờ I/O — interrupt() không đủ
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}

        // Đánh thức send() nếu đang wait() chờ responseBuffer
        synchronized (this) {
            requestInProgress = false;
            this.notifyAll();
        }

        this.notificationHandlers.clear();
        System.out.println("Đã dừng luồng lắng nghe thành công.");
    }

    // Gán hàm xử lý notification (Tương thích ngược)
    @Deprecated
    public void setNotificationHandler(Consumer<Notification> handler) {
        notificationHandlers.clear();
        if (handler != null) {
            notificationHandlers.add(handler);
        }
    }

    // Đăng ký nhận thông báo realtime
    public void addNotificationHandler(Consumer<Notification> handler) {
        if (handler != null && !notificationHandlers.contains(handler)) {
            notificationHandlers.add(handler);
        }
    }

    // Hủy đăng ký nhận thông báo realtime
    public void removeNotificationHandler(Consumer<Notification> handler) {
        if (handler != null) {
            notificationHandlers.remove(handler);
        }
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

    public Response setAutoBid(int auctionId, double maxBid, double increment) {
        return send(RequestType.SET_AUTO_BID, new AutoBidPayload(auctionId, maxBid, increment));
    }

    public Response cancelAutoBid(int auctionId) {
        return send(RequestType.CANCEL_AUTO_BID, auctionId);
    }
}