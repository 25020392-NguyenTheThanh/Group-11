package com.auction.network;

public enum RequestType {
    LOGIN, // đăng nhập , kiểm tra username /password
    REGISTER, // đăng ký tài khoản mới
    LOGOUT, // đăng xuất
    GET_AUCTIONS, // lấy danh sách phiên đấu giá đang hoạt động
    GET_AUCTION_DETAIL, // xem chi tiết 1 phiên đấu giá cụ thể
    PLACE_BID, // đặt giá thầu cho phiên
    CREATE_AUCTION, // tạo phiên đấu giá mới
    CREATE_ITEM, // thêm sản phẩm mới vào hệ thống
    GET_MY_ITEMS, // lấy danh sách sản phầm đã tạo
    DELETE_ITEM, // xóa sản phẩm
    UPDATE_ITEM, // sửa sản phẩm
    ADD_TO_WATCHLIST, // thêm vào danh sách theo dõi
    REMOVE_FROM_WATCHLIST, // xóa khỏi danh sách theo dõi
    TOP_UP, // nạp tiền tài khoản
    CONFIRM_PAYMENT, // xác nhận thanh toán
    SET_AUTO_BID, // đăng kí auto bid
    CANCEL_AUTO_BID, // hủy auto bid
    CHANGE_PASSWORD, // đổi mật khẩu
    VERIFY_EMAIL, // xác thực email quên mật khẩu
    RESET_PASSWORD ,// khôi phục mật khẩu mới

//admin

    ADMIN_GET_ALL_USERS,          // Xem danh sách toàn bộ user
    ADMIN_BAN_USER,              // Khóa tài khoản
    ADMIN_UNBAN_USER,            // Mở khóa tài khoản
    ADMIN_DELETE_USER,           // Xóa hẳn user
    ADMIN_RESET_USER_PASSWORD,   // Đặt lại mật khẩu cho user
    ADMIN_GET_ALL_AUCTIONS,      // Xem tất cả phiên đấu giá
    ADMIN_CANCEL_AUCTION,        // Hủy phiên đấu giá
    ADMIN_GET_ALL_ITEMS,         // Xem toàn bộ sản phẩm
    ADMIN_FORCE_DELETE_ITEM,     // Xóa sản phẩm bất kỳ (kể cả IN_AUCTION)
    ADMIN_GET_STATS,             // Thống kê hệ thống
    ADMIN_GET_AUDIT_LOG,         // Xem log bảo mật
    ADMIN_GET_ACTIVE_SESSIONS,   // Xem session đang hoạt động
    ADMIN_KICK_USER,             // Ngắt kết nối 1 user
    DECLINE_PAYMENT              // Từ chối thanh toán và hủy phiên
}

