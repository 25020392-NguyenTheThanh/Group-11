package com.auction.server.handler;

import com.auction.data.DataManager;
import com.auction.manager.AuctionManager;
import com.auction.manager.ItemManager;
import com.auction.manager.UserManager;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.user.User;
import com.auction.network.AdminPayload;
import com.auction.network.Notification;
import com.auction.network.Request;
import com.auction.network.Response;
import com.auction.security.AuditLogger;
import com.auction.server.AuctionServer;
import com.auction.server.ClientHandler;

import java.util.List;

/**
 * Xử lý tất cả request dành cho Admin:
 * GET_ALL_USERS, BAN/UNBAN/DELETE/RESET_PASSWORD user,
 * GET_ALL_AUCTIONS, CANCEL_AUCTION,
 * GET_ALL_ITEMS, FORCE_DELETE_ITEM, APPROVE_ITEM,
 * GET_STATS, GET_AUDIT_LOG, GET_ACTIVE_SESSIONS, KICK_USER
 */
public class AdminHandler {

    public static final AuditLogger audit = AuditLogger.getInstance();

    // Guard dùng chung — trả về Response lỗi nếu không phải Admin, null nếu ok
    private static Response requireAdmin(ClientHandler handler) {
        User u = handler.getLoggedInUser();
        if (u == null) return Response.error("Bạn cần đăng nhập!");
        if (!"ADMIN".equalsIgnoreCase(u.getRole())) return Response.error("Từ chối: Quyền Admin là bắt buộc!");
        return null;
    }

    public static Response handleGetAllUsers(ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        return Response.ok(UserManager.getInstance().getAllUsers());
    }

    public static Response handleBanUser(Request request, ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Payload không hợp lệ!");

        boolean ok = UserManager.getInstance().banUser(p.targetId, p.reason);
        if (!ok) return Response.error("Không thể khóa tài khoản.");

        audit.logAdminBan(handler.getClientIp(), handler.getLoggedInUser().getId(), p.targetId, p.reason);

        // Kick user đang online
        handler.getServer().getConnectedClients().stream()
                .filter(c -> c.getLoggedInUser() != null && c.getLoggedInUser().getId() == p.targetId)
                .findFirst()
                .ifPresent(c -> {
                    c.sendNotification(new Notification("FORCE_LOGOUT", "Tài khoản bị khóa bởi Admin. Lý do: " + p.reason));
                    c.setLoggedInUser(null);
                });
        return Response.ok("Đã khóa tài khoản thành công!");
    }

    public static Response handleUnbanUser(Request request, ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        boolean ok = UserManager.getInstance().unbanUser(p.targetId);
        return ok ? Response.ok("Đã mở khóa tài khoản thành công!") : Response.error("Không thể mở khóa tài khoản.");
    }

    public static Response handleDeleteUser(Request request, ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        try {
            boolean ok = UserManager.getInstance().deleteUserPermanently(p.targetId);
            return ok ? Response.ok("Đã xóa vĩnh viễn tài khoản khỏi hệ thống!") : Response.error("Xóa tài khoản thất bại.");
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response handleResetUserPassword(Request request, ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        User user = UserManager.getInstance().findUserById(p.targetId);
        if (user == null) return Response.error("Không tìm thấy user mục tiêu.");

        boolean ok = UserManager.getInstance().updatePassword(user, p.newPassword);
        return ok ? Response.ok("Đã đặt lại mật khẩu cho user thành công!") : Response.error("Không thể đặt lại mật khẩu.");
    }

    public static Response handleGetAllAuctions(ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        return Response.ok(AuctionManager.getInstance().getAllAuctionsIncludingInactive());
    }

    public static Response handleCancelAuction(Request request, ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        boolean ok = AuctionManager.getInstance().cancelAuctionByAdmin(p.targetId, p.reason);
        if (!ok) return Response.error("Không thể hủy phiên đấu giá.");

        handler.getServer().broadcast(new Notification("AUCTION_CANCELED_BY_ADMIN",
                "Phiên đấu giá #" + p.targetId + " đã bị hủy bởi Admin."));
        return Response.ok("Đã hủy phiên đấu giá thành công!");
    }

    public static Response handleGetAllItems(ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        return Response.ok(ItemManager.getInstance().getAllItemsInSystem());
    }

    public static Response handleForceDeleteItem(Request request, ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        boolean ok = ItemManager.getInstance().forceDeleteItemByAdmin(p.targetId);
        return ok ? Response.ok("Admin đã xóa cưỡng chế sản phẩm thành công!") : Response.error("Không thể xóa sản phẩm.");
    }

    public static Response handleApproveItem(Request request, ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        boolean ok = ItemManager.getInstance().updateItemStatus(p.targetId, ItemStatus.AVAILABLE);
        if (!ok) return Response.error("Không thể duyệt sản phẩm.");

        // Thông báo cho Seller nếu online
        Item item = ItemManager.getInstance().findItem(p.targetId);
        if (item != null && AuctionServer.getInstance() != null) {
            String msg = String.format("Sản phẩm [%s] của bạn đã được Admin phê duyệt!", item.getName());
            AuctionServer.getInstance().getConnectedClients().stream()
                    .filter(c -> c.getLoggedInUser() != null && c.getLoggedInUser().getId() == item.getOwnerId())
                    .findFirst()
                    .ifPresent(c -> c.sendNotification(new Notification("PRODUCT_APPROVED", msg)));
        }
        return Response.ok("Duyệt sản phẩm thành công!");
    }

    public static Response handleApproveItemsBatch(Request request, ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");
        if (p.targetIds == null || p.targetIds.isEmpty()) return Response.error("Danh sách sản phẩm trống!");

        int successCount = 0;
        for (int itemId : p.targetIds) {
            boolean ok = ItemManager.getInstance().updateItemStatus(itemId, ItemStatus.AVAILABLE);
            if (ok) {
                successCount++;
                // Thông báo cho Seller nếu online
                Item item = ItemManager.getInstance().findItem(itemId);
                if (item != null && AuctionServer.getInstance() != null) {
                    String msg = String.format("Sản phẩm [%s] của bạn đã được Admin phê duyệt!", item.getName());
                    AuctionServer.getInstance().getConnectedClients().stream()
                            .filter(c -> c.getLoggedInUser() != null && c.getLoggedInUser().getId() == item.getOwnerId())
                            .findFirst()
                            .ifPresent(c -> c.sendNotification(new Notification("PRODUCT_APPROVED", msg)));
                }
            }
        }
        return Response.ok(String.format("Đã duyệt thành công %d/%d sản phẩm!", successCount, p.targetIds.size()));
    }

    public static Response handleGetStats(ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        return Response.ok(DataManager.getInstance().getSystemStatisticsDto());
    }

    public static Response handleGetAuditLog(ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        List<String> logs = AuditLogger.getInstance().getRecentLogs(200);
        return Response.ok(logs);
    }

    public static Response handleGetActiveSessions(ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;

        List<String> sessions = handler.getServer().getConnectedClients().stream()
                .map(c -> {
                    User u = c.getLoggedInUser();
                    return "Session ID: " + c.hashCode() + " | User: " +
                            (u != null ? u.getUsername() + " (" + u.getRole() + ")" : "GUEST");
                })
                .toList();
        return Response.ok(sessions);
    }

    public static Response handleKickUser(Request request, ClientHandler handler) {
        Response guard = requireAdmin(handler);
        if (guard != null) return guard;
        if (!(request.getPayload() instanceof AdminPayload p)) return Response.error("Dữ liệu Admin không hợp lệ!");

        ClientHandler target = handler.getServer().getConnectedClients().stream()
                .filter(c -> c.getLoggedInUser() != null && c.getLoggedInUser().getId() == p.targetId)
                .findFirst().orElse(null);

        if (target == null) return Response.error("User hiện không online hoặc không tìm thấy session.");

        target.sendNotification(new Notification("KICKED", "Bạn đã bị sút ra khỏi hệ thống bởi Admin."));
        target.closeConnection();
        return Response.ok("Đã đá user thành công.");
    }
}