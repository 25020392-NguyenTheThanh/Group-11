package com.auction.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý session token cho người dùng đã đăng nhập.
 *
 * Mỗi lần đăng nhập thành công → cấp một token ngẫu nhiên (32 byte / 256-bit).
 * Mọi request cần xác thực phải kèm token này.
 *
 * Token hết hạn sau SESSION_TTL_SECONDS giây không hoạt động (idle timeout).
 */
public class SessionManager {

    private static final int TOKEN_BYTES        = 32;           // 256-bit token
    private static final long SESSION_TTL_MS    = 30 * 60_000L; // 30 phút idle timeout

    // token -> SessionEntry
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

    /**
     * Tạo session mới cho userId. Trả về token.
     */
    public String createSession(int userId, String role) {
        // Vô hiệu session cũ của user này (nếu có) — chỉ cho phép 1 session/user
        invalidateByUserId(userId);

        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        sessions.put(token, new SessionEntry(userId, role, System.currentTimeMillis()));
        System.out.println("[Session] Tạo session mới cho userId=" + userId);
        return token;
    }

    /**
     * Kiểm tra token hợp lệ và làm mới thời gian hết hạn.
     * @return SessionEntry nếu hợp lệ, null nếu không hợp lệ / hết hạn
     */
    public SessionEntry validateAndRefresh(String token) {
        if (token == null) return null;
        SessionEntry entry = sessions.get(token);
        if (entry == null) return null;
        long idleMs = System.currentTimeMillis() - entry.lastActiveMs;
        if (idleMs > SESSION_TTL_MS) {
            sessions.remove(token);
            System.out.println("[Session] Token hết hạn, đã xóa.");
            return null;
        }
        entry.lastActiveMs = System.currentTimeMillis(); // sliding window
        return entry;
    }

    /**
     * Hủy session theo token (đăng xuất).
     */
    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /**
     * Hủy tất cả session của một userId.
     */
    public void invalidateByUserId(int userId) {
        sessions.entrySet().removeIf(e -> e.getValue().userId == userId);
    }

    /**
     * Thông tin session.
     */
    public static class SessionEntry {
        public final int userId;
        public final String role;
        public volatile long lastActiveMs;

        SessionEntry(int userId, String role, long lastActiveMs) {
            this.userId = userId;
            this.role   = role;
            this.lastActiveMs = lastActiveMs;
        }
    }
}
