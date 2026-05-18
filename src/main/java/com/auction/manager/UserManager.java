package com.auction.manager;

import com.auction.data.DataManager;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.util.List;

public class UserManager {

    private static volatile UserManager instance;

    private UserManager() {}

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager();
                    //users.add(new Bidder(2,"nguyenvana","123","jooj",35));
                    //users.add(new Seller(5,"nguyenvanb","1234","feff"));
                }
            }
        }
        return instance;
    }

    /**
     * Đăng ký người dùng mới — lưu vào MySQL.
     * return User vừa tạo, hoặc null nếu username/email đã tồn tại.
     */
    public synchronized User register(String username, String password,
                                      String email, String role) {
        boolean ok = DataManager.getInstance().registerUser(username, password, email, role);
        if (!ok) {
            System.out.println("Đăng ký thất bại: username/email đã tồn tại — " + username);
            return null;
        }
        // Đọc lại từ DB để trả về object có id thật
        return DataManager.getInstance().authenticate(username, password);
    }

    /**
     * Đăng nhập — xác thực qua MySQL.
     * return User nếu đúng thông tin, null nếu sai.
     */
    public User login(String username, String password) {
        User user = DataManager.getInstance().authenticate(username, password);
        if (user == null) {
            System.out.println("Đăng nhập thất bại: " + username);
            return null;
        }
        user.login(password); // đánh dấu authenticated = true
        return user;
    }

    // Lấy toàn bộ danh sách user (dùng cho Admin).
    public List<User> getUsers() {
        return DataManager.getInstance().getAllUsers();
    }

    // Tìm user theo username.
    public User findUser(String username) {
        return getUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    @Deprecated
    public void loadFromDisk() {
        System.out.println("[UserManager] loadFromDisk() đã bị bỏ — dữ liệu lấy từ MySQL.");
    }

    @Deprecated
    public void saveToDisk() {
        System.out.println("[UserManager] saveToDisk() đã bị bỏ — dữ liệu lưu vào MySQL.");
    }
}