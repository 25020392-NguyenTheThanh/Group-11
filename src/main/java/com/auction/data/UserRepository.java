package com.auction.data;

import com.auction.model.user.User;
import com.auction.security.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private static boolean tableExists(DatabaseMetaData meta, String tableName) throws SQLException {
        try (ResultSet rs = meta.getTables(null, null, tableName, null)) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getTables(null, null, tableName.toUpperCase(), null)) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), null)) {
            if (rs.next()) return true;
        }
        return false;
    }

    private static boolean columnExists(DatabaseMetaData meta, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase())) {
            if (rs.next()) return true;
        }
        return false;
    }

    static {
        try (Connection con = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            try (Statement st = con.createStatement()) {
                // 1. Kiểm tra/thêm các cột cho bảng users
                if (!columnExists(meta, "users", "status")) {
                    st.execute("ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
                    System.out.println("[Database Migration] Added status column to users table.");
                }
                if (!columnExists(meta, "users", "ban_reason")) {
                    st.execute("ALTER TABLE users ADD COLUMN ban_reason VARCHAR(255) DEFAULT NULL");
                    System.out.println("[Database Migration] Added ban_reason column to users table.");
                }

                // 2. Kiểm tra/thêm các cột cho bảng bidders
                if (!columnExists(meta, "bidders", "has_topped_up")) {
                    st.execute("ALTER TABLE bidders ADD COLUMN has_topped_up TINYINT NOT NULL DEFAULT 0");
                    System.out.println("[Database Migration] Added has_topped_up column to bidders table.");
                }
                if (!columnExists(meta, "bidders", "last_top_up_time")) {
                    st.execute("ALTER TABLE bidders ADD COLUMN last_top_up_time DATETIME DEFAULT NULL");
                    System.out.println("[Database Migration] Added last_top_up_time column to bidders table.");
                }

                // 3. Kiểm tra và tạo bảng bidder_participated nếu chưa có
                if (!tableExists(meta, "bidder_participated")) {
                    st.execute("CREATE TABLE bidder_participated (" +
                            "bidder_id INT NOT NULL," +
                            "auction_id INT NOT NULL," +
                            "PRIMARY KEY (bidder_id, auction_id)," +
                            "CONSTRAINT fk_participated_bidder FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                            "CONSTRAINT fk_participated_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE ON UPDATE CASCADE" +
                            ")");
                    System.out.println("[Database Migration] Created table bidder_participated.");
                }

                // 4. Kiểm tra và tạo bảng bidder_won nếu chưa có
                if (!tableExists(meta, "bidder_won")) {
                    st.execute("CREATE TABLE bidder_won (" +
                            "bidder_id INT NOT NULL," +
                            "auction_id INT NOT NULL," +
                            "PRIMARY KEY (bidder_id, auction_id)," +
                            "CONSTRAINT fk_won_bidder FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                            "CONSTRAINT fk_won_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE ON UPDATE CASCADE" +
                            ")");
                    System.out.println("[Database Migration] Created table bidder_won.");
                }

                // 5. Kiểm tra và tạo bảng bid_watchlist nếu chưa có
                if (!tableExists(meta, "bid_watchlist")) {
                    st.execute("CREATE TABLE bid_watchlist (" +
                            "bidder_id INT NOT NULL," +
                            "auction_id INT NOT NULL," +
                            "PRIMARY KEY (bidder_id, auction_id)," +
                            "CONSTRAINT fk_watchlist_bidder FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                            "CONSTRAINT fk_watchlist_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE ON UPDATE CASCADE" +
                            ")");
                    System.out.println("[Database Migration] Created table bid_watchlist.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Migration] Error: " + e.getMessage());
        }
    }

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
        // Chỉ tìm theo username, KHÔNG đưa password vào SQL để tránh timing attack
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    // Verify hash — không so sánh plaintext
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
            String hashedPassword = PasswordUtil.hash(password);
            String sqlUser = "INSERT INTO users (username, password, email, role, status, ban_reason) VALUES (?,?,?,?,'PENDING','Tài khoản đang chờ duyệt từ Admin.')";
            try (PreparedStatement ps = con.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, hashedPassword); // lưu hash, không lưu plaintext
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

    // Tìm user theo username — query trực tiếp, không load toàn bảng
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

    // Tìm user theo email — query trực tiếp, không load toàn bảng
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

    // Tìm user theo id — query trực tiếp, không load toàn bảng
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

    public boolean addBidderBalance(int bidderId, double amount) {
        String sql = "UPDATE bidders SET balance = balance + ? WHERE user_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, bidderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserRepository] addBidderBalance lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean deductBidderBalance(int bidderId, double amount) {
        String sql = "UPDATE bidders SET balance = balance - ? WHERE user_id = ? AND balance >= ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, bidderId);
            ps.setDouble(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserRepository] deductBidderBalance lỗi: " + e.getMessage());
            return false;
        }
    }

    public double getBidderBalance(int bidderId) {
        String sql = "SELECT balance FROM bidders WHERE user_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserRepository] getBidderBalance lỗi: " + e.getMessage());
        }
        return -1; // -1 if error or not found
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
            default -> null;
        };
        if (sql == null) return;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public boolean markBidderToppedUp(int bidderId) {
        String sql = "UPDATE bidders SET has_topped_up = 1, last_top_up_time = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserRepository] markBidderToppedUp lỗi: " + e.getMessage());
            return false;
        }
    }

    /** Cập nhật mật khẩu mới (đã hash) xuống DB cho user có id tương ứng. */
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserRepository] updatePassword lỗi: " + e.getMessage());
            return false;
        }
    }
    // Thêm hàm này để xử lý Ban/Unban user
    public boolean updateStatus(int userId, String status, String reason) {
        String sql = "UPDATE users SET status = ?, ban_reason = ? WHERE id = ?";
        try (java.sql.Connection conn = com.auction.data.DatabaseConnection.getConnection(); // Thay bằng class kết nối DB của bạn
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Thêm hàm này để xóa vĩnh viễn user
    public boolean delete(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (java.sql.Connection conn = com.auction.data.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("constraint") || msg.contains("foreign key")) {
                throw new RuntimeException("Không thể xóa tài khoản này vì tài khoản đang có dữ liệu liên kết (sản phẩm, giao dịch hoặc phiên đấu giá). Vui lòng sử dụng chức năng Khóa tài khoản thay thế!");
            }
            throw new RuntimeException("Lỗi cơ sở dữ liệu: " + e.getMessage());
        }
    }

    // Thêm hàm này để lấy danh sách log bảo mật (Audit Logs)
    public java.util.List<String> getAuditLogs() {
        java.util.List<String> logs = new java.util.ArrayList<>();
        // Nếu bạn có bảng lịch sử hệ thống riêng (ví dụ: system_logs, audit_logs) thì query từ bảng đó.
        // Dưới đây là phương án fallback đọc nhanh từ lịch sử lý do ban của các user:
        String sql = "SELECT username, status, ban_reason FROM users WHERE ban_reason IS NOT NULL";
        try (java.sql.Connection conn = com.auction.data.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                logs.add("User [" + rs.getString("username") + "] thay đổi trạng thái sang "
                        + rs.getString("status") + " | Lý do: " + rs.getString("ban_reason"));
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
}