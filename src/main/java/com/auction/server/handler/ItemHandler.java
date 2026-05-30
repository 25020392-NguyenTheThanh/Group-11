package com.auction.server.handler;

import com.auction.manager.AuctionManager;
import com.auction.manager.ItemManager;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.*;
import com.auction.pattern.factory.*;
import com.auction.security.InputValidator;
import com.auction.server.ClientHandler;

/**
 * Xử lý tất cả request liên quan đến sản phẩm:
 * CREATE_ITEM, GET_MY_ITEMS, DELETE_ITEM, UPDATE_ITEM
 */
public class ItemHandler {

    public static Response handleCreateItem(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể tạo sản phẩm");
        if (!(request.getPayload() instanceof CreateItemPayload p)) return Response.error("Payload không hợp lệ!");
        if (p.startingPrice < 100) return Response.error("Giá khởi điểm tối thiểu là 100!");

        String name = InputValidator.sanitize(p.name);
        if (name.isBlank()) return Response.error("Tên sản phẩm không hợp lệ!");

        ItemFactory factory = resolveFactory(p.type, p.artist, p.year, p.brand);
        Item item = ItemManager.getInstance().createItem(factory, user.getId(), p.name, p.description, p.startingPrice, p.imageUrl);
        if (item == null) return Response.error("Không thể tạo sản phẩm, vui lòng thử lại.");

        handler.sendNotification(new Notification("PRODUCT_APPROVED",
                String.format("Sản phẩm [%s] của bạn đã được phê duyệt và tạo thành công!", item.getName())));
        return Response.ok(item);
    }

    public static Response handleGetMyItems(ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        return Response.ok(ItemManager.getInstance().getByOwner(user.getId()));
    }

    public static Response handleDeleteItem(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể xóa sản phẩm");
        if (!(request.getPayload() instanceof Integer itemId)) return Response.error("Dữ liệu xóa sản phẩm không hợp lệ!");

        Item item = ItemManager.getInstance().findItem(itemId);
        if (item == null) return Response.error("Sản phẩm không tồn tại");
        if (item.getOwnerId() != user.getId()) return Response.error("Bạn không phải chủ sở hữu của sản phẩm này");
        if (item.getStatus() == ItemStatus.IN_AUCTION) return Response.error("Không thể xóa sản phẩm đang ở trạng thái IN_AUCTION");

        boolean ok = ItemManager.getInstance().deleteItem(itemId);
        return ok ? Response.ok("Xóa sản phẩm thành công") : Response.error("Không thể xóa sản phẩm khỏi cơ sở dữ liệu");
    }

    public static Response handleUpdateItem(Request request, ClientHandler handler) {
        User user = handler.getLoggedInUser();
        if (user == null) return Response.error("Chưa đăng nhập");
        if (!(user instanceof Seller)) return Response.error("Chỉ Seller mới có thể sửa sản phẩm");
        if (!(request.getPayload() instanceof UpdateItemPayload p)) return Response.error("Dữ liệu sửa sản phẩm không hợp lệ!");
        if (p.startingPrice < 100) return Response.error("Giá khởi điểm phải lớn hơn hoặc bằng 100!");

        Item existing = ItemManager.getInstance().findItem(p.id);
        if (existing == null) return Response.error("Sản phẩm không tồn tại");
        if (existing.getOwnerId() != user.getId()) return Response.error("Bạn không phải chủ sở hữu của sản phẩm này");

        // Kiểm tra trạng thái cho phép sửa
        Response statusCheck = validateItemEditableStatus(existing);
        if (statusCheck != null) return statusCheck;

        ItemFactory factory = resolveFactory(p.type, p.artist, p.year, p.brand);
        Item updated = factory.createItem(p.id, user.getId(), p.name, p.description, p.startingPrice, p.imageUrl);
        updated.setStatus(existing.getStatus());

        boolean ok = ItemManager.getInstance().updateItem(updated);
        if (ok) {
            // Đồng bộ item mới vào Auction đang chạy (nếu có)
            AuctionManager.getInstance().getAuctions().stream()
                    .filter(a -> a.getItem().getId() == existing.getId())
                    .findFirst()
                    .ifPresent(a -> a.setItem(updated));
            return Response.ok("Cập nhật sản phẩm thành công");
        }
        return Response.error("Không thể cập nhật sản phẩm trong cơ sở dữ liệu");
    }

    // Kiểm tra item có đang ở trạng thái được phép sửa không
    private static Response validateItemEditableStatus(Item item) {
        if (item.getStatus() == ItemStatus.IN_AUCTION) {
            Auction auction = AuctionManager.getInstance().getAuctions().stream()
                    .filter(a -> a.getItem().getId() == item.getId())
                    .findFirst().orElse(null);
            if (auction != null && auction.getStatus() != AuctionStatus.OPEN)
                return Response.error("Chỉ có thể sửa sản phẩm khi phiên đấu giá chưa bắt đầu (OPEN).");
        } else if (item.getStatus() != ItemStatus.AVAILABLE) {
            return Response.error("Chỉ có thể sửa sản phẩm ở trạng thái AVAILABLE hoặc phiên chưa bắt đầu.");
        }
        return null; // ok
    }

    // Tạo đúng factory dựa trên loại sản phẩm
    private static ItemFactory resolveFactory(String type, String artist, int year, String brand) {
        return switch (type.toUpperCase()) {
            case "ART"     -> new ArtFactory(artist);
            case "VEHICLE" -> new VehicleFactory(year);
            default        -> new ElectronicsFactory(brand);
        };
    }
}