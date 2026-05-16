package com.auction.manager;

import com.auction.data.UserDAO;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class UserManager {

    private static volatile UserManager instance ;
    private static List<User> users = new ArrayList<>() ; // Danh sách người dùng
    private int userCounter = 1 ;
    private final UserDAO userDAO = new UserDAO();

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
        if (userDAO.existsByUsername(username)) {
            System.out.println("Username đã tồn tại");
            return null;
        }
        User newUser = switch (role) {
            case "BIDDER" -> new Bidder(userCounter, username, password, email, 0);
            case "SELLER" -> new Seller(userCounter, username, password, email);
            default       -> new Admin(userCounter, username, password, email);
        };

        boolean saved = userDAO.save(newUser); // lưu vào MySQL
        if (saved){
            users.add(newUser);
            userCounter++ ;
        }
        return saved ? newUser : null ;
    }

    // đăng nhập - tìm trong MySQL
    public User login(String username, String password) {
        User user = userDAO.findByUsernameAndPassword(username , password);
        if (user != null) {
            user.login(password);
        }
        return user ;
    }

    // load tất cả các users từ MySQL khi server khởi động
    public void loadFromDB(){
        users = userDAO.findAll();
        if (!users.isEmpty()){
            userCounter = users.stream().mapToInt(User::getId).max().getAsInt() + 1;
        }
        System.out.println("Đã load " + users.size() + " users từ MySQL");
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

//    public void loadFromDisk(){
//        File f = new File("usermanager.dat");
//
//        if (f.exists()){
//            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))){
//                UserManager loaded = (UserManager) ois.readObject();
//                this.users = loaded.users ; // gán danh sách users từ object vừa đọc từ file vào obj hiện tại
//                this.userCounter = loaded.userCounter ;
//            } catch (IOException | ClassNotFoundException e){
//                System.out.println("Lỗi khi load UserManager : " + e.getMessage());
//            }
//        } else {
//            System.out.println("File usermanager.dat chưa tồn tại, tạo user mặc định");
//            System.out.println("File chưa tồn tại, tạo mới");
//            users = new ArrayList<>();  // chỉ tạo list rỗng
//            userCounter = 1;
//        }
//    }
//
//    public void saveToDisk() {
//        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("usermanager.dat"))) {
//            oos.writeObject(this);
//            System.out.println("Đã lưu UserManager xuống file");
//        } catch (IOException e) {
//            System.out.println("Lỗi khi lưu UserManager: " + e.getMessage());
//        }
//    }

}

