package com.auction.server;

import com.auction.model.user.User;
import com.auction.network.Notification;
import com.auction.network.Request;
import com.auction.network.Response;
import com.auction.pattern.observer.Observer;

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
    public void run(){}

    @Override
    public void send(String message){}

    public void sendNotification(Notification notification){}

    public void setLoggedInUser(User user){}

    public User getLoggedInUser(){return loggedInUser;}
}
