package com.auction.data;

import com.auction.model.user.User;
import com.auction.security.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final DatabaseConnection db;
    private final UserMapper mapper;

    public UserRepository() {
        this.db     = DatabaseConnection.getInstance();
        this.mapper = new UserMapper();
    }

    /**
     * Xác thực đăng nhập.
     * return User nếu đúng thông tin, {@code null} nếu sai.
     */
    public User authenticate(String username, String password) {
        // Chỉ query theo username, xác minh password bằng PasswordUtil.verify()
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    // verify() hỗ trợ cả plain text cũ lẫn hashed mới
                    if (PasswordUtil.verify(password, storedPassword)) {
                        return mapper.map(con, rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserRepository] authenticate lỗi: " + e.getMessage());
        }
        return null;
    }

    // ─── LỖI 1+2+3 FIX: register dùng 1 connection, có transaction, kiểm tra cả username lẫn email ──
    /**
     * Trả về "ok" nếu thành công.
     * Trả về "USERNAME_EXISTS", "EMAIL_EXISTS", hoặc mô tả lỗi nếu thất bại.
     */
    public String register(String username, String password, String email, String role) {
        // Kiểm tra trùng trước khi mở transaction
        if (usernameExists(username)) return "USERNAME_EXISTS";
        if (emailExists(email))       return "EMAIL_EXISTS";

        Connection con = null;
        try {
            con = db.getConnection();
            con.setAutoCommit(false);   // ← bắt đầu transaction

            // Bước 1: INSERT vào bảng users
            int newId;
            String sqlUser = "INSERT INTO users (username, password, email, role) VALUES (?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, PasswordUtil.hash(password));  // hash trước khi lưu DB
                ps.setString(3, email);
                ps.setString(4, role.toUpperCase());
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) {
                    con.rollback();
                    return "Không lấy được ID người dùng mới";
                }
                newId = keys.getInt(1);
            }

            // Bước 2: INSERT vào bidders/sellers — CÙNG connection, CÙNG transaction
            insertRoleRecord(con, newId, role);

            con.commit();   // ← commit cả 2 bước
            return "ok";

        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ignored) {}
            System.err.println("[UserRepository] register lỗi: " + e.getMessage());
            return "Lỗi DB: " + e.getMessage();
        } finally {
            if (con != null) try {
                con.setAutoCommit(true);
                con.close();
            } catch (SQLException ignored) {}
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
            System.err.println("[UserRepository] findAll lỗi: " + e.getMessage());
        }
        return list;
    }

    // ─── LỖI 4 FIX: implement đầy đủ, không còn stub trả false ──────────────
    public boolean addSellerRevenue(int sellerId, double amount) {
        String sql = "UPDATE sellers SET revenue = revenue + ? WHERE user_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, sellerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserRepository] addSellerRevenue lỗi: " + e.getMessage());
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
            System.err.println("[UserRepository] updateBidderBalance lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean addToWatchlist(int bidderId, int auctionId) {
        String sql = "INSERT IGNORE INTO bid_watchlist (bidder_id, auction_id) VALUES (?, ?)";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeFromWatchlist(int bidderId, int auctionId) {
        String sql = "DELETE FROM bid_watchlist WHERE bidder_id = ? AND auction_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────
    private boolean usernameExists(String username) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    private boolean emailExists(String email) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    /** INSERT bản ghi phụ vào bidders/sellers — PHẢI dùng cùng connection với INSERT users */
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
}