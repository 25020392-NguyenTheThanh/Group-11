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

-- Bảng Items (sản phẩm đấu giá)
CREATE TABLE items (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id INT NOT NULL, -- id seller tạo ra item
    name VARCHAR(200) NOT NULL,
    description TEXT, -- Mô tả sản phẩm
    starting_price DECIMAL(18,2) NOT NULL,
    category ENUM('ART', 'ELECTRONICS', 'VEHICLE') NOT NULL,
    status ENUM('AVAILABLE','IN_AUCTION','SOLD') NOT NULL DEFAULT 'AVAILABLE',
    image_url VARCHAR(255) DEFAULT NULL,
-- Thuộc tính riêng của từng loại
	artist VARCHAR(100),  -- chỉ art
    brand VARCHAR(100),  -- chỉ electronics
	manufacture_year SMALLINT, -- chỉ vehicle 
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- tự lưu thời gian tạo item
    CONSTRAINT fk_item_seller FOREIGN KEY (owner_id) REFERENCES users(id) 
	ON DELETE RESTRICT -- Không cho xoá user nếu còn item
    ON UPDATE CASCADE -- Néu user.id thay đổi thì item.owner_id thay đổi theo
);

-- Bảng Auction (phiên đấu giá)
CREATE TABLE auctions (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL UNIQUE, -- item được đem đấu giá
    current_highest_bid DECIMAL(18,2) NOT NULL, -- Giá đấu cao nhất hiện tại
    current_winner_id INT DEFAULT NULL, -- Mới tạo là null
    status ENUM('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED') NOT NULL DEFAULT 'OPEN',
    start_time DATETIME NOT NULL, -- Thời gian bắt đầu đấu giá
	end_time DATETIME NOT NULL, -- Thời gian kết thúc đấu giá
    min_bid_step DECIMAL(18,2) NOT NULL DEFAULT 0.00, -- Giá đấu đặt tối thiểu
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, 
    CONSTRAINT fk_auction_item FOREIGN KEY (item_id) REFERENCES items(id)
	ON DELETE RESTRICT 
	ON UPDATE CASCADE,
    CONSTRAINT fk_auction_winner  FOREIGN KEY (current_winner_id) REFERENCES users(id) -- Người thắng hiện tại phải tồn tại trong users
	ON DELETE SET NULL -- Nếu user thắng bị xoá thì current_winner_id = NULL
    ON UPDATE CASCADE 
);

-- Bảng bid_transactions (lịch sử đặt giá — BidTransaction)
CREATE TABLE bid_transactions (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_id INT NOT NULL,
    bidder_name VARCHAR(50) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    bid_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bid_auction FOREIGN KEY (auction_id) REFERENCES auctions(id)
	ON DELETE CASCADE 
    ON UPDATE CASCADE,
    CONSTRAINT fk_bid_bidder  FOREIGN KEY (bidder_id)  REFERENCES users(id)
	ON DELETE RESTRICT 
    ON UPDATE CASCADE,
    INDEX idx_auction_time (auction_id, bid_time)
);

-- Bảng bidder_watchlist (danh sách phiên theo dõi)
CREATE TABLE bid_watchlist (
	bidder_id INT NOT NULL,
    auction_id INT NOT NULL,
    PRIMARY KEY (bidder_id, auction_id),
    CONSTRAINT fk_wl_bidder  FOREIGN KEY (bidder_id)  REFERENCES users(id)   
    ON DELETE CASCADE,
    CONSTRAINT fk_wl_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) 
    ON DELETE CASCADE
);

-- Bảng bidder_participated  (phiên đã từng đặt giá)
CREATE TABLE bidder_participated (
    bidder_id INT NOT NULL,
    auction_id INT NOT NULL,
    PRIMARY KEY (bidder_id, auction_id),
    CONSTRAINT fk_part_bidder  FOREIGN KEY (bidder_id)  REFERENCES users(id)   
    ON DELETE CASCADE,
    CONSTRAINT fk_part_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) 
    ON DELETE CASCADE
);

-- Bảng bidder_won (phiên đã thắng)
CREATE TABLE bidder_won (
	bidder_id  INT NOT NULL,
    auction_id INT NOT NULL,
    PRIMARY KEY (bidder_id, auction_id),
    CONSTRAINT fk_won_bidder FOREIGN KEY (bidder_id) REFERENCES users(id)   
    ON DELETE CASCADE,
    CONSTRAINT fk_won_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) 
    ON DELETE CASCADE
);