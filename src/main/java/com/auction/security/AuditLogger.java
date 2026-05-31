package com.auction.security;

import com.auction.data.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Ghi log các sự kiện bảo mật quan trọng vào database.
 *
 * Các sự kiện được log:
 *  - Đăng nhập thành công / thất bại
 *  - IP bị khóa
 *  - Đăng xuất
 *  - Các hành động nhạy cảm (đặt giá, tạo phiên đấu giá...)
 *
 * Format hiển thị: [YYYY-MM-DD HH:mm:ss] [LEVEL] [EVENT] IP=x userId=x message
 */
public class AuditLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static volatile AuditLogger instance;

    private AuditLogger() {
    }

    public static AuditLogger getInstance() {
        if (instance == null) {
            synchronized (AuditLogger.class) {
                if (instance == null) instance = new AuditLogger();
            }
        }
        return instance;
    }

    // ── Auth events ──────────────────────────────────────────────────────────
    public void logLoginSuccess(String ip, String username)  { log("INFO",  "LOGIN_SUCCESS",  ip, "username=" + username); }
    public void logLoginFailure(String ip, String username)  { log("WARN",  "LOGIN_FAILURE",  ip, "username=" + username); }
    public void logRegister(String ip, String username)      { log("INFO",  "REGISTER",       ip, "username=" + username); }
    public void logLogout(String ip, int userId)             { log("INFO",  "LOGOUT",         ip, "userId=" + userId); }
    public void logIpBlocked(String ip)                      { log("WARN",  "IP_BLOCKED",     ip, "Quá nhiều lần thất bại"); }
    public void logSuspicious(String ip, String detail)      { log("ERROR", "SUSPICIOUS",     ip, detail); }
    public void logPasswordChanged(String ip, int userId)    { log("INFO",  "PASSWD_CHANGED", ip, "userId=" + userId); }

    // ── Bid events ───────────────────────────────────────────────────────────
    public void logBid(String ip, int userId, int auctionId, double amount) {
        log("INFO", "PLACE_BID", ip, "userId=" + userId + " auctionId=" + auctionId + " amount=" + amount);
    }

    // ── Admin events ──────────────────────────────────────────────────────────
    public void logAdminBan(String ip, int adminId, int targetUserId, String reason) {
        log("WARN", "ADMIN_BAN", ip, "adminId=" + adminId + " target=" + targetUserId + " reason=" + reason);
    }
    public void logAdminUnban(String ip, int adminId, int targetUserId) {
        log("INFO", "ADMIN_UNBAN", ip, "adminId=" + adminId + " target=" + targetUserId);
    }
    public void logAdminDeleteUser(String ip, int adminId, int targetUserId) {
        log("WARN", "ADMIN_DELETE_USER", ip, "adminId=" + adminId + " target=" + targetUserId);
    }
    public void logAdminCancelAuction(String ip, int adminId, int auctionId, String reason) {
        log("WARN", "ADMIN_CANCEL_AUCTION", ip, "adminId=" + adminId + " auctionId=" + auctionId + " reason=" + reason);
    }
    public void logAdminForceDeleteItem(String ip, int adminId, int itemId) {
        log("WARN", "ADMIN_DELETE_ITEM", ip, "adminId=" + adminId + " itemId=" + itemId);
    }
    public void logAdminResetPassword(String ip, int adminId, int targetUserId) {
        log("WARN", "ADMIN_RESET_PASSWD", ip, "adminId=" + adminId + " target=" + targetUserId);
    }
    public void logAdminKickUser(String ip, int adminId, int targetUserId) {
        log("INFO", "ADMIN_KICK", ip, "adminId=" + adminId + " target=" + targetUserId);
    }
    public void logAdminGetStats(String ip, int adminId) {
        log("INFO", "ADMIN_GET_STATS", ip, "adminId=" + adminId);
    }

    /**
     * Đọc N dòng log cuối cùng để Admin xem — trả về List<String>.
     */
    public List<String> getRecentLogs(int maxLines) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT log_time, level, event, ip, detail FROM audit_logs ORDER BY id DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maxLines);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("log_time");
                    String logTimeStr = ts != null ? ts.toLocalDateTime().format(FMT) : "";
                    String level = rs.getString("level");
                    String event = rs.getString("event");
                    String ip = rs.getString("ip");
                    String detail = rs.getString("detail");

                    String line = String.format("[%s] [%s] [%s] IP=%s %s",
                            logTimeStr, level, event, ip != null ? ip : "", detail != null ? detail : "");
                    // Sắp xếp ngược lại để các log mới nhất hiển thị cuối danh sách (tương đương đọc cuối file)
                    list.add(0, line);
                }
            }
        } catch (SQLException e) {
            list.add("[AuditLogger] Không đọc được log từ database: " + e.getMessage());
        }
        return list;
    }

    private synchronized void log(String level, String event, String ip, String detail) {
        LocalDateTime now = LocalDateTime.now();
        String line = String.format("[%s] [%s] [%s] IP=%s %s",
                now.format(FMT), level, event, ip, detail);
        System.out.println(line);

        // Lưu vào database
        String sql = "INSERT INTO audit_logs (log_time, level, event, ip, detail) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            ps.setString(2, level);
            ps.setString(3, event);
            ps.setString(4, ip);
            ps.setString(5, detail);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuditLogger] Lỗi lưu log vào database: " + e.getMessage());
        }
    }
}