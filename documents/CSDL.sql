-- Xoá CSDL cũ
DROP DATABASE IF EXISTS auction_system;
-- Tạo Database auction 
CREATE DATABASE auction_system
	CHARACTER SET utf8mb4 -- Chỉ định bảng mã cho database (ở đây là Unicode đầy đủ)
    COLLATE utf8mb4_unicode_ci; -- Quy tắc so sánh theo chuẩn Unicode và ci-không pb hoa thường

USE auction_system;

-- Bảng Users (thông tin chung)
CREATE TABLE users (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
    username VARCHAR(50) NOT NULL UNIQUE, 
    password VARCHAR(255) NOT NULL,        
    email VARCHAR(100) NOT NULL UNIQUE,
    role ENUM('BIDDER','SELLER','ADMIN') NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1, -- Trạng thái tài khoản (hoạt động trả giá trị 1)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP -- Tự lưu thời gian tạo tài khoản
);

-- Bảng Bidders (thông tin riêng của bidders)
CREATE TABLE bidders (
    user_id INT NOT NULL PRIMARY KEY, 
    balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_bidder_user FOREIGN KEY (user_id) REFERENCES users(id) -- Ràng buộc bidders.user_id phải tồn tại trong users.id
	ON DELETE CASCADE -- Néu user.id bị xoá thì bidders.user_id xoá theo
    ON UPDATE CASCADE  -- Néu user.id thay đổi thì bidders.user_id thay đổi theo
);

-- Bảng Sellers (thông tin riêng của sellers)
CREATE TABLE sellers (
	user_id INT NOT NULL PRIMARY KEY,
    revenue DECIMAL(18,2)  NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_seller_user FOREIGN KEY (user_id) REFERENCES users(id) -- Ràng buộc sellers.user_id phải tồn tại trong users.id
	ON DELETE CASCADE -- Néu user.id bị xoá thì sellers.user_id xoá theo
    ON UPDATE CASCADE -- Néu user.id thay đổi thì sellers.user_id thay đổi theo
);

