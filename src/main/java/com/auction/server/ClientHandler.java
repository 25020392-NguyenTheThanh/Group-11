package com.auction.server;

import com.auction.model.user.User;
import com.auction.network.Notification;
import com.auction.network.Request;
import com.auction.network.Response;
import com.auction.pattern.observer.Observer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable , Observer {
    private final Socket socket;
    private final AuctionServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User loggedInUser; // user đang đăng nhập qua kết nối này
    public ClientHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
    }
    // viết tiếp

    @Override
    public void run(){
        try {
            // QUAN TRỌNG: phải tạo out TRƯỚC in — tránh deadlock khi cả 2 đầu cùng chờ
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // flush ngay để bên client tạo được ObjectInputStream
            in  = new ObjectInputStream(socket.getInputStream());

            System.out.println("Client kết nối: " + socket.getInetAddress());

            // Vòng lặp đọc request liên tục cho đến khi client ngắt kết nối
            while (true) {
                Request request = (Request) in.readObject(); // chờ client gửi
                Response response = RequestProcessor.process(request, this);
                sendResponse(response); // gửi kết quả về cho client
            }

        } catch (IOException e) {
            // Client ngắt kết nối — bình thường, không cần in stacktrace
            System.out.println("Client ngắt kết nối: " + socket.getInetAddress());
        } catch (ClassNotFoundException e) {
            System.err.println("Lỗi deserialize request: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    private synchronized void sendResponse(Response response) {
        try {
            out.writeObject(response);
            out.flush();
            out.reset(); // reset cache — quan trọng khi gửi object đã thay đổi
        } catch (IOException e) {
            System.err.println("Lỗi gửi response: " + e.getMessage());
        }
    }
    private void cleanup() {
        try {
            if (in  != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        server.removeClient(this); // xóa khỏi danh sách connectedClients
        System.out.println("Đã dọn dẹp kết nối: " + socket.getInetAddress());
    }

    @Override
    public void send(String message){
        sendNotification(new Notification("BID_UPDATE" , message));
    }

    public synchronized void sendNotification(Notification notification){
        try {
            if (out != null){
                out.writeObject(notification);
                out.flush();
                out.reset();
            }
        } catch (IOException e){
            System.err.println("Lỗi gửi notification : " + e.getMessage());
        }
    }

    public void setLoggedInUser(User user){ this.loggedInUser = user ;}

    public User getLoggedInUser(){return loggedInUser;}
}
// out.reset(): khi gửi cùng một object nhiều lần ,
// Java ObjectOutputStream cache lại object đó.
// Nếu không reset(), lần gửi sau nó sẽ gửi một reference đến object cũ thay vì object mới