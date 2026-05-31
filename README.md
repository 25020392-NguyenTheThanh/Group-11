<img width="1639" height="674" alt="image" src="https://github.com/user-attachments/assets/2755fc94-8926-42d6-ae0d-dda461291725" /># Group-11# Hệ thống Đấu Giá Trực Tuyến — Nhóm 11

Hệ thống đấu giá trực tuyến theo mô hình Client–Server, hỗ trợ nhiều người dùng đồng thời với giao diện JavaFX. Người dùng có thể đăng ký tài khoản, đăng sản phẩm, tạo phiên đấu giá, đặt giá thủ công hoặc tự động, và theo dõi kết quả theo thời gian thực.

---

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Giao diện | JavaFX 21.0.6 |
| Cơ sở dữ liệu | MySQL 8 (cloud) |
| JDBC Driver | mysql-connector-j 8.3.0 |
| Build tool | Maven 3 |
| Unit Test | JUnit Jupiter 5.9.3 |
| UI Components | ControlsFX 11.2.1, BootstrapFX 0.4.0, TilesFX 21.0.9 |
| UI Extras | FormsFX 11.6.0, ValidatorFX 0.6.1, Ikonli 12.3.1, FXGL 11.17 |
| Bảo mật mật khẩu | SHA-256 + Salt ngẫu nhiên (java.security) |

## Yêu cầu cài đặt

- Java 21 trở lên
- Maven 3.8 trở lên
- Kết nối Internet (database trên cloud) hoặc MySQL 8 local nếu tự host
- Hệ điều hành: Windows / macOS / Linux

---

## Cấu trúc thư mục

```
src/main/java/
├── com/auction/
│   ├── client/          # ServerConnection, ServerDiscoveryClient (UDP)
│   ├── data/            # Repository, DataManager, SchemaInitializer, DatabaseConnection
│   ├── exception/       # AuctionClosedException, AuthenticationException, InvalidBidException
│   ├── manager/         # AuctionManager, UserManager, ItemManager
│   ├── model/
│   │   ├── auction/     # Auction, BidProcessor, AutoBidEngine, AntiSnipeGuard, AuctionStatus, AutoBidConfig, BidTransaction
│   │   ├── entity/      # Entity (base class)
│   │   ├── item/        # Item, Electronics, Art, Vehicle, ItemStatus
│   │   └── user/        # User, Bidder, BidderProfile, Seller, Admin
│   ├── network/         # Request, Response, Notification, RequestType và các Payload class
│   ├── pattern/
│   │   ├── observer/    # Subject, Observer interfaces
│   │   └── factory/     # ItemFactory (abstract), ArtFactory, ElectronicsFactory, VehicleFactory
│   ├── security/        # PasswordUtil, AuditLogger, RateLimiter, SessionManager, InputValidator
│   └── server/
│       ├── handler/     # AdminHandler, AuctionHandler, AuthHandler, BidderHandler, ItemHandler
│       └── ...          # AuctionServer, ClientHandler, AuctionTimer, RequestProcessor, ServerDiscovery
└── com/example/group11/
    ├── MainApplication.java   # Entry point Client (JavaFX)
    ├── Launcher.java
    └── controller/            # JavaFX Controllers cho từng màn hình
```

---

## Cài đặt cơ sở dữ liệu

### Dùng database cloud (mặc định)

Dự án đã cấu hình sẵn kết nối tới MySQL cloud. Không cần cài đặt thêm — chạy thẳng được.

### Tự host MySQL local

Nếu muốn chạy với MySQL local, đặt các biến môi trường trước khi chạy:

```bash
# Windows (Command Prompt)
set DB_URL=jdbc:mysql://localhost:3306/auction_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh&useSSL=false&allowPublicKeyRetrieval=true
set DB_USER=root
set DB_PASS=your_password

# macOS / Linux
export DB_URL="jdbc:mysql://localhost:3306/auction_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh&useSSL=false&allowPublicKeyRetrieval=true"
export DB_USER=root
export DB_PASS=your_password
```

Tạo database:

```sql
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Sau đó chạy file `documents/CSDL.sql` để tạo toàn bộ bảng (users, bidders, sellers, items, auctions, bid_transactions, bidder_participated, bidder_won, bid_watchlist).

> Khi server khởi động lần đầu, `SchemaInitializer` tự động thêm các cột còn thiếu nếu cần.

---

## Cách chạy

### 1. Build project

```bash
mvn clean package -DskipTests
```

### 2. Chạy Server

Server **không dùng JavaFX** — chạy bằng lệnh sau:

```bash
mvn exec:java -Dexec.mainClass="com.auction.server.AuctionServer"
```

Hoặc sau khi build jar:

```bash
java -cp target/Group-11-1.0-SNAPSHOT.jar com.auction.server.AuctionServer
```

Server lắng nghe trên:
- **TCP port 9999** — kết nối client
- **UDP port 8888** — auto-discovery trong mạng LAN

### 3. Chạy Client

```bash
mvn javafx:run
```

Ứng dụng sẽ tự động tìm server trong mạng LAN qua UDP broadcast (timeout 3 giây mỗi lượt). Không cần nhập IP thủ công.

Để chạy nhiều client cùng lúc, mở nhiều terminal và chạy lại lệnh trên mỗi terminal.

### 4. Chạy Tests

```bash
mvn test
```

---

## Hướng dẫn chạy Server/Client theo thứ tự

1. *(Nếu dùng MySQL local)* Đảm bảo MySQL đang chạy và đã tạo database + import schema.
2. Khởi động **Server** trước (terminal 1).
3. Đợi log `"Server started on port 9999"` xuất hiện.
4. Mở **Client** (terminal 2, 3, ...) — mỗi terminal là một người dùng riêng.
5. Đăng ký tài khoản hoặc đăng nhập để sử dụng.

> **Lưu ý mạng LAN:** Server và Client phải cùng mạng LAN để auto-discovery UDP hoạt động. Nếu chạy trên các subnet khác nhau hoặc router chặn broadcast, discovery có thể không tìm thấy server.

---

## Danh sách chức năng đã hoàn thành

### Bidder (Người đặt giá)
- Đăng ký, đăng nhập, đăng xuất
- Xem danh sách phiên đấu giá đang chạy
- Xem chi tiết phiên, lịch sử giá thầu
- Đặt giá thủ công
- Đặt giá tự động (Auto-Bid) với ngưỡng tối đa và bước giá
- Hủy Auto-Bid
- Thêm/xóa phiên theo dõi (Watchlist)
- Nạp tiền vào tài khoản
- Xác nhận thanh toán sau khi thắng
- Đổi mật khẩu
- Đặt lại mật khẩu (giả lập — chưa gửi email thật)
- Nhận thông báo real-time (bị vượt giá, thắng phiên, số dư thay đổi)

### Seller (Người bán)
- Đăng ký/đăng nhập
- Đăng sản phẩm (Electronics, Art, Vehicle)
- Chỉnh sửa/xóa sản phẩm chưa vào đấu giá
- Tạo phiên đấu giá từ sản phẩm đã được duyệt
- Theo dõi diễn biến phiên (lượt đặt giá đầu tiên, surge, milestone giá)
- Xem thống kê doanh thu

### Admin
- Xem và quản lý toàn bộ người dùng
- Khoá/mở khoá tài khoản
- Xoá tài khoản
- Đặt lại mật khẩu cho người dùng
- Duyệt sản phẩm
- Xem và hủy phiên đấu giá bất kỳ
- Xóa sản phẩm bất kỳ (kể cả đang trong đấu giá)
- Xem thống kê hệ thống
- Xem audit log bảo mật
- Xem session đang hoạt động, ngắt kết nối người dùng

### Hệ thống
- **Auto-Bid Engine:** khi có bid mới, server tự động kích hoạt engine — tìm config có `maxBid` cao nhất còn đủ ngân sách, đặt bid bằng `giá hiện tại + increment`, delay 3 giây giữa các lượt; tự hủy config khi vượt ngưỡng tối đa
- **Anti-Sniping:** tự động gia hạn phiên 60s nếu có bid trong 30s cuối (tối đa 5 lần)
- **Bảo mật:** Rate limiting theo IP, Session token, Input validation, Audit log
- **Mật khẩu:** băm bằng SHA-256 + Salt ngẫu nhiên (không lưu plain text)
- Xử lý concurrent bidding an toàn (`synchronized`, `ConcurrentHashMap`, `CopyOnWriteArrayList`)
- Tự động hoàn tiền khi bị vượt giá
- **Crash recovery:** serialize/deserialize trạng thái server ra file `.dat`
- **Auto-discovery:** client tự tìm server qua UDP broadcast trong mạng LAN


## Link báo cáo & video demo

- **Báo cáo PDF: https://drive.google.com/file/d/122ygmpu6IIl9OSGYzT1OuVBBkR1L7-Gp/view?usp=sharing
- **Video demo:
