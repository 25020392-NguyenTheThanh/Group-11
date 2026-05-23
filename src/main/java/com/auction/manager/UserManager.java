package com.auction.manager;

import com.auction.data.DataManager;
import com.auction.model.user.User;

import java.util.List;

public class UserManager {

    private static volatile UserManager instance;
    private UserManager() {}

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) instance = new UserManager();
            }
        }
        return instance;
    }

    /**
     * Đăng ký người dùng mới.
     * Trả về User nếu thành công.
     * Ném IllegalArgumentException với thông báo chi tiết nếu thất bại.
     */
    public synchronized User register(String username, String password, String email, String role) {
        String result = DataManager.getInstance().registerUser(username, password, email, role);
        return switch (result) {
            case "ok" -> {
                User created = DataManager.getInstance().authenticate(username, password);
                if (created != null) created.login(password);
                yield created;
            }
            case "USERNAME_EXISTS" -> throw new IllegalArgumentException("Username '" + username + "' đã tồn tại!");
            case "EMAIL_EXISTS" -> throw new IllegalArgumentException("Email '" + email + "' đã được đăng ký!");
            default -> throw new IllegalArgumentException("Đăng ký thất bại: " + result);
        };
    }

    public User login(String username, String password) {
        User user = DataManager.getInstance().authenticate(username, password);
        if (user == null) return null;
        user.login(password);
        return user;
    }

    public List<User> getUsers() { return DataManager.getInstance().getAllUsers(); }

    public User findUser(String username) {
        return getUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst().orElse(null);
    }
    // Tìm user theo id.
    public User findUserById(int id) {
        return getUsers().stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElse(null);
    }
    @Deprecated
    public void loadFromDisk() {
        System.out.println("[UserManager] loadFromDisk() đã bị bỏ — dữ liệu lấy từ MySQL.");
    }

    @Deprecated
    public void saveToDisk() {
        System.out.println("[UserManager] saveToDisk() đã bị bỏ — dữ liệu lưu vào MySQL.");}

    /** Đổi mật khẩu: hash rồi lưu DB, đồng thời cập nhật object trong RAM. */
    public boolean updatePassword(User user, String newPassword) {
        boolean ok = DataManager.getInstance().updatePassword(user.getId(), newPassword);
        if (ok) {
            // Cập nhật RAM để các thao tác tiếp theo trong session dùng đúng password
            user.setPassWord(com.auction.security.PasswordUtil.hash(newPassword));
        }
        return ok;
    }

}