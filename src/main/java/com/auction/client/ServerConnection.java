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

public class ServerConnection {
    private static final String HOST = "localhost";
    private static final int PORT = 9999 ;

    private static ServerConnection instance = new ServerConnection();
    private Socket socket ;
    private ObjectOutputStream out ; // gửi dữ liệu đi
    private ObjectInputStream in ; // nhận dữ liệu về
    private Thread listenerThread ;

    private Consumer<Notification> notificationHandler ;

    private ServerConnection(){}

    public static ServerConnection getInstance(){
        return instance ;
    }

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT); // client tạo socket và kết nối với server đang chạy tại HOST : PORT
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        startListening(); // lắng nghe notification từ server
        System.out.println("Đã kết nối tới server");
    }
    // gửi request và chờ response - dùng cho các thao tác ( login , bid , ...)
    public synchronized Response send(RequestType type , Object payload){
        try {
            out.writeObject(new Request(type , payload));
            out.flush();
            out.reset(); // xóa cache ObjectOutputStream đang giữ
            return (Response) in.readObject(); // chờ server phản hồi
        } catch (IOException | ClassNotFoundException e){
            System.err.println("Lỗi gửi request : " + e.getMessage());
            return Response.error("Mất kết nối tới server");
        }
    }

    private void startListening(){
        listenerThread = new Thread(() -> {
           while (!socket.isClosed()){
               try {
                   Object obj = in.readObject();
                   if ( obj instanceof Notification n && notificationHandler != null){
                       javafx.application.Platform.runLater(() -> notificationHandler.accept(n));
                   }
               } catch (IOException | ClassNotFoundException e){
                   if (!socket.isClosed()){
                       System.err.println("Listener lỗi : " + e.getMessage());
                   }
                   break ;
               }
           }
        });
        listenerThread.setDaemon(true); // tự tắt khi đóng app
        listenerThread.start();
    }

    public void setNotificationHandler(Consumer<Notification> handler){
        this.notificationHandler = handler ;
    }

    public void disconnect() throws IOException {
        if (socket != null) socket.close();
    }
}
