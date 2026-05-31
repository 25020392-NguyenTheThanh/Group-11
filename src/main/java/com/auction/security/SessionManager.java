package com.auction.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Quản lý session token sau đăng nhập.
 * - Token 256-bit ngẫu nhiên, hết hạn sau 30 phút không hoạt động (sliding window).
 * - Mỗi user chỉ có 1 session duy nhất — đăng nhập mới hủy session cũ.
 */
public class SessionManager {

    private static final int  TOKEN_BYTES     = 32;
    private static final long SESSION_TTL_MS  = 30 * 60_000L; // 30 phút

    private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private static volatile SessionManager instance;
    private final SecureRandom random = new SecureRandom();

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) instance = new SessionManager();
            }
        }
        return instance;
    }

    /** Tạo session mới — hủy session cũ của cùng userId nếu có */
    public String createSession(int userId, String role) {
        invalidateByUserId(userId);
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new SessionEntry(userId, role, System.currentTimeMillis()));
        System.out.println("[Session] Tạo session cho userId=" + userId + " role=" + role);
        return token;
    }

    /** Validate token và gia hạn thời gian (sliding window) */
    public SessionEntry validateAndRefresh(String token) {
        if (token == null) return null;
        SessionEntry entry = sessions.get(token);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.lastActiveMs > SESSION_TTL_MS) {
            sessions.remove(token);
            System.out.println("[Session] Token hết hạn, đã xóa.");
            return null;
        }
        entry.lastActiveMs = System.currentTimeMillis();
        return entry;
    }

    /** Hủy session theo token (đăng xuất) */
    public void invalidate(String token) {
        if (token != null) sessions.remove(token);
    }

    /** Hủy tất cả session của một userId (đổi mật khẩu, Admin reset) */
    public void invalidateByUserId(int userId) {
        sessions.entrySet().removeIf(e -> e.getValue().userId == userId);
    }

    /** Số session đang hoạt động */
    public int activeCount() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> now - e.getValue().lastActiveMs > SESSION_TTL_MS);
        return sessions.size();
    }

    /** Danh sách session cho Admin xem */
    public List<String> getActiveSummaries() {
        long now = System.currentTimeMillis();
        return sessions.entrySet().stream()
                .filter(e -> now - e.getValue().lastActiveMs <= SESSION_TTL_MS)
                .map(e -> "userId=" + e.getValue().userId
                        + " role=" + e.getValue().role
                        + " idle=" + (now - e.getValue().lastActiveMs) / 1000 + "s"
                        + " token=" + e.getKey().substring(0, 8) + "…")
                .collect(Collectors.toList());
    }

    public static class SessionEntry {
        public final int    userId;
        public final String role;
        public volatile long lastActiveMs;

        SessionEntry(int userId, String role, long lastActiveMs) {
            this.userId       = userId;
            this.role         = role;
            this.lastActiveMs = lastActiveMs;
        }
    }
}
