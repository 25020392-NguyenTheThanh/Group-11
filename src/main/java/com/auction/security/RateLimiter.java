package com.auction.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiter — giới hạn số lần thử đăng nhập từ một IP để chống brute-force.
 *
 * Cơ chế: đếm số lần thất bại trong cửa sổ thời gian (window).
 * Nếu vượt ngưỡng MAX_ATTEMPTS → khóa IP đó trong LOCKOUT_SECONDS giây.
 */
public class RateLimiter {

    private static final int MAX_ATTEMPTS      = 5;    // số lần thất bại tối đa
    private static final int LOCKOUT_SECONDS   = 60;   // thời gian khóa (giây)
    private static final int WINDOW_SECONDS    = 300;  // cửa sổ đếm (5 phút)

    // Map: IP -> số lần thất bại
    private final ConcurrentHashMap<String, AtomicInteger> failureCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long>          lockoutTime  = new ConcurrentHashMap<>();

    private static volatile RateLimiter instance;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RateLimiter-Cleaner");
        t.setDaemon(true);
        return t;
    });

    private RateLimiter() {
        cleaner.scheduleAtFixedRate(this::cleanup, WINDOW_SECONDS, WINDOW_SECONDS, TimeUnit.SECONDS);
    }

    public static RateLimiter getInstance() {
        if (instance == null) {
            synchronized (RateLimiter.class) {
                if (instance == null) instance = new RateLimiter();
            }
        }
        return instance;
    }

    /** @return true nếu IP đang bị khóa */
    public boolean isBlocked(String ip) {
        Long lockedAt = lockoutTime.get(ip);
        if (lockedAt == null) return false;
        long elapsed = (System.currentTimeMillis() - lockedAt) / 1000;
        if (elapsed >= LOCKOUT_SECONDS) {
            lockoutTime.remove(ip);
            failureCount.remove(ip);
            return false;
        }
        return true;
    }

    /**
     * Kiểm tra và trả thông báo nếu bị block.
     * @return null nếu được phép, chuỗi thông báo nếu bị chặn.
     */
    public String check(String ip) {
        if (!isBlocked(ip)) return null;
        long secs = getRemainingLockSeconds(ip);
        return "Quá nhiều lần thử thất bại! Vui lòng đợi " + secs + " giây.";
    }

    public void recordFailure(String ip) {
        AtomicInteger count = failureCount.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int attempts = count.incrementAndGet();
        if (attempts >= MAX_ATTEMPTS) {
            lockoutTime.put(ip, System.currentTimeMillis());
            AuditLogger.getInstance().logIpBlocked(ip);
        }
    }

    public void recordSuccess(String ip) {
        failureCount.remove(ip);
        lockoutTime.remove(ip);
    }

    public long getRemainingLockSeconds(String ip) {
        Long lockedAt = lockoutTime.get(ip);
        if (lockedAt == null) return 0;
        long elapsed = (System.currentTimeMillis() - lockedAt) / 1000;
        return Math.max(0, LOCKOUT_SECONDS - elapsed);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        lockoutTime.entrySet().removeIf(e -> (now - e.getValue()) / 1000 >= LOCKOUT_SECONDS);
        failureCount.entrySet().removeIf(e -> !lockoutTime.containsKey(e.getKey()));
    }
}