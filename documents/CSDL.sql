-- Bảng Users (thông tin chung)
CREATE TABLE users (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
    username VARCHAR(50) NOT NULL UNIQUE, 
    password VARCHAR(255) NOT NULL,        
    email VARCHAR(100) NOT NULL UNIQUE,
    role ENUM('BIDDER','SELLER','ADMIN') NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1, -- Trạng thái tài khoản (hoạt động trả giá trị 1)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- Tự lưu thời gian tạo tài khoản
);

-- Bảng Bidders (thông tin riêng của bidders)
CREATE TABLE bidders (
    user_id INT NOT NULL PRIMARY KEY, 
    balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    last_top_up_time TIMESTAMP NULL DEFAULT NULL,
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
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- tự lưu thời gian tạo item
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
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
    bid_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    bid_type ENUM('MANUAL','AUTO') NOT NULL DEFAULT 'MANUAL',
    CONSTRAINT fk_bid_auction FOREIGN KEY (auction_id) REFERENCES auctions(id)
	ON DELETE CASCADE 
    ON UPDATE CASCADE,
    CONSTRAINT fk_bid_bidder  FOREIGN KEY (bidder_id)  REFERENCES users(id)
	ON DELETE RESTRICT 
    ON UPDATE CASCADE,
    INDEX idx_auction_time (auction_id, bid_time)
);

-- Các truy vấn xử lý bid (chạy sau khi đã tạo bảng)
-- 1. Xem các bid trùng (cùng bidder + amount + timestamp)
SELECT bidder_id, bidder_name, amount, bid_time, COUNT(*) as cnt
FROM bid_transactions
WHERE auction_id = 8
GROUP BY bidder_id, bidder_name, amount, bid_time
HAVING COUNT(*) > 1;

-- 2. Xóa bid trùng, chỉ giữ id nhỏ nhất (Sửa lỗi MySQL Target Table)
DELETE b1 FROM bid_transactions b1
INNER JOIN bid_transactions b2
WHERE b1.auction_id = 8
  AND b2.auction_id = 8
  AND b1.bidder_id = b2.bidder_id
  AND b1.amount = b2.amount
  AND b1.bid_time = b2.bid_time
  AND b1.id > b2.id; -- Xóa dòng có ID lớn hơn, giữ lại ID nhỏ nhất
