package com.auction.data;

import com.auction.model.user.User;
import com.auction.security.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    static {
        SchemaInitializer.run();
    }

    private final DatabaseConnection db;
    private final UserMapper mapper;

    public UserRepository() {
        this.db     = DatabaseConnection.getInstance();
        this.mapper = new UserMapper();
    }

    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (PasswordUtil.verify(password, storedHash)) {
                        return mapper.map(con, rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserRepository] authenticate lỗi: " + e.getMessage());
        }
        return null;
    }

    /**
     * Trả về "ok" nếu thành công.
     * Trả về "USERNAME_EXISTS", "EMAIL_EXISTS", hoặc mô tả lỗi nếu thất bại.
     */
    public String register(String username, String password, String email, String role) {
        if (usernameExists(username)) return "USERNAME_EXISTS";
        if (emailExists(email))       return "EMAIL_EXISTS";

        Connection con = null;
        try {
            con = db.getConnection();
            con.setAutoCommit(false);

            int newId;
            String sql = "INSERT INTO users (username, password, email, role, status, ban_reason) VALUES (?,?,?,?,'PENDING','Tài khoản đang chờ duyệt từ Admin.')";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, PasswordUtil.hash(password));
                ps.setString(3, email);
                ps.setString(4, role.toUpperCase());
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) { con.rollback(); return "Không lấy được ID người dùng mới"; }
                newId = keys.getInt(1);
            }

            insertRoleRecord(con, newId, role);
            con.commit();
            return "ok";

        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ignored) {}
            System.err.println("[UserRepository] register lỗi: " + e.getMessage());
            return "Lỗi DB: " + e.getMessage();
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ignored) {}
        }
    }

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

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper.map(con, rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserRepository] findByUsername lỗi: " + e.getMessage());
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper.map(con, rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserRepository] findByEmail lỗi: " + e.getMessage());
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapper.map(con, rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserRepository] findById lỗi: " + e.getMessage());
        }
        return null;
    }

    public boolean addSellerRevenue(int sellerId, double amount) {
        return executeUpdate("UPDATE sellers SET revenue = revenue + ? WHERE user_id = ?",
                ps -> { ps.setDouble(1, amount); ps.setInt(2, sellerId); }, "addSellerRevenue");
    }

    public boolean updateBidderBalance(int bidderId, double newBalance) {
        return executeUpdate("UPDATE bidders SET balance = ? WHERE user_id = ?",
                ps -> { ps.setDouble(1, newBalance); ps.setInt(2, bidderId); }, "updateBidderBalance");
    }

    public boolean addBidderBalance(int bidderId, double amount) {
        return executeUpdate("UPDATE bidders SET balance = balance + ? WHERE user_id = ?",
                ps -> { ps.setDouble(1, amount); ps.setInt(2, bidderId); }, "addBidderBalance");
    }

    public boolean deductBidderBalance(int bidderId, double amount) {
        return executeUpdate("UPDATE bidders SET balance = balance - ? WHERE user_id = ? AND balance >= ?",
                ps -> { ps.setDouble(1, amount); ps.setInt(2, bidderId); ps.setDouble(3, amount); }, "deductBidderBalance");
    }

    public double getBidderBalance(int bidderId) {
        String sql = "SELECT balance FROM bidders WHERE user_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            System.err.println("[UserRepository] getBidderBalance lỗi: " + e.getMessage());
        }
        return -1;
    }

    public boolean addToWatchlist(int bidderId, int auctionId) {
        return executeUpdate("INSERT IGNORE INTO bid_watchlist (bidder_id, auction_id) VALUES (?, ?)",
                ps -> { ps.setInt(1, bidderId); ps.setInt(2, auctionId); }, "addToWatchlist");
    }

    public boolean removeFromWatchlist(int bidderId, int auctionId) {
        return executeUpdate("DELETE FROM bid_watchlist WHERE bidder_id = ? AND auction_id = ?",
                ps -> { ps.setInt(1, bidderId); ps.setInt(2, auctionId); }, "removeFromWatchlist");
    }

    public boolean markBidderToppedUp(int bidderId) {
        return executeUpdate("UPDATE bidders SET has_topped_up = 1, last_top_up_time = CURRENT_TIMESTAMP WHERE user_id = ?",
                ps -> ps.setInt(1, bidderId), "markBidderToppedUp");
    }

    public boolean updatePassword(int userId, String newPassword) {
        return executeUpdate("UPDATE users SET password = ? WHERE id = ?",
                ps -> { ps.setString(1, PasswordUtil.hash(newPassword)); ps.setInt(2, userId); }, "updatePassword");
    }

    public boolean updateStatus(int userId, String status, String reason) {
        return executeUpdate("UPDATE users SET status = ?, ban_reason = ? WHERE id = ?",
                ps -> { ps.setString(1, status); ps.setString(2, reason); ps.setInt(3, userId); }, "updateStatus");
    }

    public boolean delete(int userId) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserRepository] delete lỗi: " + e.getMessage());
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("constraint") || msg.contains("foreign key"))
                throw new RuntimeException("Không thể xóa tài khoản này vì tài khoản đang có dữ liệu liên kết (sản phẩm, giao dịch hoặc phiên đấu giá). Vui lòng sử dụng chức năng Khóa tài khoản thay thế!");
            throw new RuntimeException("Lỗi cơ sở dữ liệu: " + e.getMessage());
        }
    }

    public List<String> getAuditLogs() {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT username, status, ban_reason FROM users WHERE ban_reason IS NOT NULL";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                logs.add("User [" + rs.getString("username") + "] thay đổi trạng thái sang "
                        + rs.getString("status") + " | Lý do: " + rs.getString("ban_reason"));
        } catch (SQLException e) {
            System.err.println("[UserRepository] getAuditLogs lỗi: " + e.getMessage());
        }
        return logs;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private boolean executeUpdate(String sql, StatementSetter setter, String methodName) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setter.set(ps);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserRepository] " + methodName + " lỗi: " + e.getMessage());
            return false;
        }
    }

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

    private void insertRoleRecord(Connection con, int userId, String role) throws SQLException {
        String sql = switch (role.toUpperCase()) {
            case "BIDDER" -> "INSERT INTO bidders (user_id, balance) VALUES (?, 0)";
            case "SELLER" -> "INSERT INTO sellers (user_id, revenue) VALUES (?, 0)";
            default -> null;
        };
        if (sql == null) return;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}