package com.auction.server.handler;

import com.auction.manager.AuctionManager;
import com.auction.manager.UserManager;
import com.auction.model.user.User;
import com.auction.network.*;
import com.auction.security.*;
import com.auction.server.AuctionServer;
import com.auction.server.ClientHandler;

/**
 * Xử lý tất cả request liên quan đến xác thực người dùng:
 * LOGIN, REGISTER, LOGOUT, VERIFY_EMAIL, RESET_PASSWORD, CHANGE_PASSWORD
 */
public class AuthHandler {

    private static final AuditLogger audit = AuditLogger.getInstance();
    private static final RateLimiter rateLimiter = RateLimiter.getInstance();

    public static Response handleLogin(Request request, ClientHandler handler) {
        String ip = handler.getClientIp();

        String blocked = rateLimiter.check(ip);
        if (blocked != null) {
            audit.logIpBlocked(ip);
            return Response.error(blocked);
        }

        if (!(request.getPayload() instanceof LoginPayload payload))
            return Response.error("Dữ liệu đăng nhập không hợp lệ!");

        if (!InputValidator.isValidUsername(payload.username)) {
            rateLimiter.recordFailure(ip);
            return Response.error("Tên đăng nhập không hợp lệ!");
        }

        try {
            User user = UserManager.getInstance().login(payload.username, payload.password);
            if (user == null)
                return Response.error("Tên đăng nhập hoặc mật khẩu không đúng!");

            // Kiểm tra đăng nhập trùng thiết bị
            if (AuctionServer.getInstance() != null) {
                for (ClientHandler existing : AuctionServer.getInstance().getConnectedClients()) {
                    if (existing == handler) continue;
                    User logged = existing.getLoggedInUser();
                    if (logged != null && logged.getId() == user.getId())
                        return Response.error("Tài khoản đang hoạt động trên một thiết bị khác. Vui lòng đăng xuất thiết bị đó trước!");
                }
            }

            if (!user.isActive()) {
                if ("PENDING".equalsIgnoreCase(user.getStatus()))
                    return Response.error("Tài khoản của bạn đang chờ duyệt từ Admin. Vui lòng quay lại sau!");
                return Response.error("Tài khoản của bạn đã bị khóa! Lý do: " + user.getBanReason());
            }

            rateLimiter.recordSuccess(ip);
            audit.logLoginSuccess(ip, payload.username);

            String token = SessionManager.getInstance().createSession(user.getId(), user.getRole());
            handler.setSessionToken(token);
            handler.setLoggedInUser(user);
            return Response.ok(user);

        } catch (Exception e) {
            return Response.error("Đăng nhập thất bại: " + e.getMessage());
        }
    }

    public static Response handleRegister(Request request, ClientHandler handler) {
        String ip = handler.getClientIp();

        String blocked = rateLimiter.check(ip);
        if (blocked != null) {
            audit.logIpBlocked(ip);
            return Response.error(blocked);
        }

        if (!(request.getPayload() instanceof RegisterPayload p))
            return Response.error("Dữ liệu đăng ký không hợp lệ!");

        if (!InputValidator.isValidUsername(p.username))
            return Response.error("Username chỉ gồm chữ, số, dấu _ và dài 3–30 ký tự!");

        String pwErr = InputValidator.getPasswordError(p.password);
        if (pwErr != null) return Response.error(pwErr);

        if (!InputValidator.isValidEmail(p.email))
            return Response.error("Email không hợp lệ!");

        if (!InputValidator.isValidRole(p.role))
            return Response.error("Vai trò không hợp lệ! Chỉ chấp nhận BIDDER hoặc SELLER.");

        try {
            User user = UserManager.getInstance().register(p.username, p.password, p.email, p.role);
            if (user == null) return Response.error("Đăng ký thất bại, vui lòng thử lại.");
            audit.logRegister(ip, p.username);
            return Response.ok(user);
        } catch (IllegalArgumentException e) {
            rateLimiter.recordFailure(ip);
            return Response.error(e.getMessage());
        } catch (Exception e) {
            return Response.error("Lỗi hệ thống khi đăng ký: " + e.getMessage());
        }
    }

    public static Response handleLogout(ClientHandler handler) {
        try {
            User user = handler.getLoggedInUser();
            if (user != null) {
                user.logout();
                AuctionManager.getInstance().removeObserverFromAllAuctions(handler);
            }
            if (handler.getSessionToken() != null) {
                SessionManager.getInstance().invalidate(handler.getSessionToken());
                handler.setSessionToken(null);
            }
            handler.setLoggedInUser(null);
            return Response.ok("Đăng xuất thành công");
        } catch (Exception e) {
            return Response.error("Lỗi đăng xuất: " + e.getMessage());
        }
    }

    public static Response handleVerifyEmail(Request request) {
        if (!(request.getPayload() instanceof String email))
            return Response.error("Email không hợp lệ!");

        User user = UserManager.getInstance().findUserByEmail(email);
        return user != null ? Response.ok(user) : Response.error("Email không tồn tại trên hệ thống!");
    }

    public static Response handleResetPassword(Request request) {
        if (!(request.getPayload() instanceof ResetPasswordPayload p))
            return Response.error("Dữ liệu khôi phục mật khẩu không hợp lệ!");

        User user = UserManager.getInstance().findUser(p.username);
        if (user == null) return Response.error("Tên đăng nhập không tồn tại!");

        boolean ok = UserManager.getInstance().updatePassword(user, p.newPassword);
        return ok ? Response.ok("Đổi mật khẩu thành công!") : Response.error("Không thể cập nhật mật khẩu mới!");
    }

    public static Response handleChangePassword(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Bạn cần đăng nhập để đổi mật khẩu!");

        if (!(request.getPayload() instanceof ChangePasswordPayload p))
            return Response.error("Dữ liệu đổi mật khẩu không hợp lệ!");

        if (!PasswordUtil.verify(p.oldPassword, user.getPassword()))
            return Response.error("Mật khẩu cũ không chính xác!");

        boolean ok = UserManager.getInstance().updatePassword(user, p.newPassword);
        return ok ? Response.ok("Đổi mật khẩu thành công!") : Response.error("Không thể cập nhật mật khẩu mới!");
    }
}