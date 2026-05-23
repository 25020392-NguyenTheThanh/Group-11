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
    SET_AUTO_BID, // đăng kí auto bid
    CANCEL_AUTO_BID // hủy auto bid
}
