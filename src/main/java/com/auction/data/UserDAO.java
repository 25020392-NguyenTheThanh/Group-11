package com.auction.data;

import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    // lưu user mới
    public boolean save(User user){
        // Chuẩn bị câu lệnh để thêm một user mới vào bảng users.
        String sql = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql , Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getRole());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int generatedId = keys.getInt(1);
                if (user instanceof Bidder b) {
                    saveBidder(conn , generatedId , b.getBalance());
                } else if (user instanceof Seller) {
                    saveSeller(conn , generatedId);
                }
            }
            return true ;
        } catch (SQLException e){
            System.err.println("Lỗi lưu user : " + e.getMessage());
            return false ;
        }
    }
    private void saveBidder(Connection conn , int userId , double balance) throws SQLException {
        String sql = "INSERT INTO bidders (user_id, balance) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, userId);
            ps.setDouble(2 , balance);
            ps.executeUpdate();
        }
    }
    private void saveSeller(Connection conn , int userId) throws SQLException {
        String sql = "INSERT INTO sellers (user_id, revenue) VALUES (?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1 , userId);
            ps.executeUpdate();
        }
    }
    // kiểm tra login - trả về user nếu đúng , null nếu sai
    public User findByUsernameAndPassword(String username , String password){
        String sql = "SELECT u.id, u.username, u.password, u.email, u.role, b.balance "
                + "FROM users u "
                + "LEFT JOIN bidders b ON u.id = b.user_id "
                + "WHERE u.username = ? AND u.password = ? AND u.active = 1";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e){
            System.err.println("Lỗi login : " + e.getMessage());
        }
        return null ;
    }

    public boolean existsByUsername(String username){
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try ( Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1 , username);
            return ps.executeQuery().next();
        } catch (SQLException e){
            return false ;
        }
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.password, u.email, u.role, b.balance "
                + "FROM users u LEFT JOIN bidders b ON u.id = b.user_id";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()){
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Lỗi load users : " + e.getMessage());
        }
        return list ;
    }

    // chuyển 1 hàng ResultSet -> User object
    private User mapRow(ResultSet rs) throws SQLException{
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email = rs.getString("email");
        String role = rs.getString("role");

        return switch (role){
            case "BIDDER" -> new Bidder(id , username , password , email , rs.getDouble("balance"));
            case "SELLER" -> new Seller(id , username , password , email);
            default       -> new Admin(id , username , password , email);
        };
    }
}
