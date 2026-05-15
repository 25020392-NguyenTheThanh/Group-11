package com.auction.manager;

import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class UserManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private static volatile UserManager instance ;
    private static List<User> users = new ArrayList<>() ; // Danh sách người dùng
    private int userCounter = 1 ;
    private UserManager(){}

    public static UserManager getInstance(){
        if (instance == null) {
            synchronized (UserManager.class){
                if (instance == null){
                    instance = new UserManager();
                    users.add(new Bidder(2,"nguyenvana","123","jooj",35));
                    users.add(new Seller(5,"nguyenvanb","1234","feff"));
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
        saveToDisk(); // THÊM: lưu ngay khi có thay đổi
        return newUser;
    }

    // đăng nhập
    public User login(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username) &&
                    u.getPassword().equals(password)) {
                u.login(password);
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

    public void loadFromDisk(){
        File f = new File("usermanager.dat");

        if (f.exists()){
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))){
                UserManager loaded = (UserManager) ois.readObject();
                this.users = loaded.users ; // gán danh sách users từ object vừa đọc từ file vào obj hiện tại
                this.userCounter = loaded.userCounter ;
            } catch (IOException | ClassNotFoundException e){
                System.out.println("Lỗi khi load UserManager : " + e.getMessage());
            }
        } else {
            System.out.println("File usermanager.dat chưa tồn tại, tạo user mặc định");
            System.out.println("File chưa tồn tại, tạo mới");
            users = new ArrayList<>();  // chỉ tạo list rỗng
            userCounter = 1;
        }
    }

    public void saveToDisk() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("usermanager.dat"))) {
            oos.writeObject(this);
            System.out.println("Đã lưu UserManager xuống file");
        } catch (IOException e) {
            System.out.println("Lỗi khi lưu UserManager: " + e.getMessage());
        }
    }

}

