package com.auction.manager;

import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private static volatile UserManager instance ;
    private List<User> users = new ArrayList<>() ; // Danh sách người dùng
    private int userCounter = 1 ;
    private UserManager(){}
    public static UserManager getInstance(){
        if (instance == null) {
            synchronized (UserManager.class){
                if (instance == null){
                    instance = new UserManager();
                }
            }
        }
        return instance ;
    }

    // đăng kí
    public synchronized User register(String username, String password,
                                      String email, String role) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                System.out.println("Username đã tồn tại");
                return null;
            }
        }
        int id = userCounter++;
        User newUser;
        if (role.equals("BIDDER")) {
            newUser = new Bidder(id, username, password, email,0);
        } else if (role.equals("SELLER")) {
            newUser = new Seller(id, username, password, email);
        } else {
            newUser = new Admin(id, username, password, email);
        }
        users.add(newUser);
        return newUser;
    }

    // đăng nhập
    public User login(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username) &&
                    u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }

    public List<User> getUsers() { return users; }

    // tìm user theo username
    public User findUser(String username){
        for (User u : users){
            if (u.getUsername().equals(username)){
                return u ;
            }
        }
        System.out.println("Username không tồn tại");
        return null ;
    }
}

