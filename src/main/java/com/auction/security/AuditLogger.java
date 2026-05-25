package com.auction.security;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ghi log các sự kiện bảo mật quan trọng ra file.
 *
 * Các sự kiện được log:
 *  - Đăng nhập thành công / thất bại
 *  - IP bị khóa
 *  - Đăng xuất
 *  - Các hành động nhạy cảm (đặt giá, tạo phiên đấu giá...)
 *
 * Format: [YYYY-MM-DD HH:mm:ss] [LEVEL] [EVENT] IP=x userId=x message
 */

public class AuditLogger {
    private static final String LOG_FILE = "audit.log";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static volatile AuditLogger instance;
    private PrintWriter writer;

    private AuditLogger() {
        try {
            // append=true để không ghi đè log cũ
            writer = new PrintWriter(new FileWriter(LOG_FILE, true), true);
        } catch (IOException e) {
            System.err.println("[AuditLogger] Không thể mở file log: " + e.getMessage());
        }
    }

    public static AuditLogger getInstance() {
        if (instance == null) {
            synchronized (AuditLogger.class) {
                if (instance == null) instance = new AuditLogger();
            }
        }
        return instance;
    }

    public void logLoginSuccess(String ip, String username) {
        log("INFO", "LOGIN_SUCCESS", ip, "username=" + username);
    }

    public void logLoginFailure(String ip, String username) {
        log("WARN", "LOGIN_FAILURE", ip, "username=" + username);
    }

    public void logIpBlocked(String ip) {
        log("WARN", "IP_BLOCKED", ip, "Quá nhiều lần đăng nhập thất bại");
    }

    public void logLogout(String ip, int userId) {
        log("INFO", "LOGOUT", ip, "userId=" + userId);
    }

    public void logBid(String ip, int userId, int auctionId, double amount) {
        log("INFO", "PLACE_BID", ip,
                "userId=" + userId + " auctionId=" + auctionId + " amount=" + amount);
    }

    public void logSuspicious(String ip, String detail) {
        log("ERROR", "SUSPICIOUS_ACTIVITY", ip, detail);
    }

    public void logRegister(String ip, String username) {
        log("INFO", "REGISTER", ip, "username=" + username);
    }

    private synchronized void log(String level, String event, String ip, String detail) {
        String line = String.format("[%s] [%s] [%s] IP=%s %s",
                LocalDateTime.now().format(FMT), level, event, ip, detail);
        System.out.println(line); // cũng in ra console để debug
        if (writer != null) {
            writer.println(line);
        }
    }
}
