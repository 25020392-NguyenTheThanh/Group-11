package com.auction.data;

import com.auction.model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final DatabaseConnection db;
    private final UserMapper mapper;

    public UserRepository() {
        this.db = DatabaseConnection.getInstance();
        this.mapper = new UserMapper();
    }

    /**
     * Xác thực đăng nhập.
     * return User nếu đúng thông tin, {@code null} nếu sai.
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper.map(con, rs);
            }
            finally {
                ps.close();
                con.close();

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Đăng ký người dùng mới.
     *return {true} nếu thành công.
     */
    public boolean register(String username, String password, String email, String role) {
        String sqlUser = "INSERT INTO users (username, password, email, role) VALUES (?,?,?,?)";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, email);
            ps.setString(4, role.toUpperCase());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) return false;
            int newId = keys.getInt(1);

            insertRoleRecord(con, newId, role);
            return true;

        } catch (SQLException e) {
            System.err.println("Lỗi đăng ký: " + e.getMessage());
            return false;
        }
    }

    // Lấy toàn bộ danh sách người dùng.
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users")) {

            while (rs.next()) list.add(mapper.map(con, rs));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Private helpers

    // Tạo bản ghi phụ trong bảng bidders hoặc sellers tùy theo role.
    private void insertRoleRecord(Connection con, int userId, String role) throws SQLException {
        String sql = switch (role.toUpperCase()) {
            case "BIDDER" -> "INSERT INTO bidders (user_id, balance) VALUES (?, 0)";
            case "SELLER" -> "INSERT INTO sellers (user_id, revenue) VALUES (?, 0)";
            default       -> null;
        };
        if (sql == null) return;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public boolean addSellerRevenue(int sellerId, double amount) {
        String sql = "UPDATE sellers SET revenue = revenue + ? WHERE user_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, sellerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi cộng doanh thu seller: " + e.getMessage());
            return false;
        }
    }

    public boolean updateBidderBalance(int bidderId, double newBalance) {
        String sql = "UPDATE bidders SET balance = ? WHERE user_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setInt(2, bidderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật số dư bidder: " + e.getMessage());
            return false;
        }
    }
}