package com.auction.data;

import com.auction.manager.UserManager;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class AuctionRepository {

    private final DatabaseConnection db;

    public AuctionRepository() {
        this.db = DatabaseConnection.getInstance();
    }

    public List<com.auction.model.auction.Auction> findAll() {
        List<com.auction.model.auction.Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions";
        try (Connection con = db.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                int itemId = rs.getInt("item_id");
                double currentHighestBid = rs.getDouble("current_highest_bid");
                int winnerId = rs.getInt("current_winner_id");
                boolean winnerIsNull = rs.wasNull();
                String statusStr = rs.getString("status");
                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                double minBidStep = rs.getDouble("min_bid_step");

                com.auction.model.item.Item item = com.auction.manager.ItemManager.getInstance().findItem(itemId);
                if (item == null) continue;

                Auction auction = new Auction(id, item, startTime, endTime, minBidStep);
                auction.restoreStatus(com.auction.model.auction.AuctionStatus.valueOf(statusStr));
                auction.restoreHighestBid(currentHighestBid);

                if (!winnerIsNull && winnerId > 0) {
                    User winner = UserManager.getInstance().findUserById(winnerId);
                    if (winner instanceof com.auction.model.user.Bidder b) {
                        auction.restoreCurrentWinner(b);
                    }
                }
                list.add(auction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Tạo phiên đấu giá mới (trạng thái OPEN).
     * return auctionId được sinh ra, hoặc {-1} nếu lỗi.
     */
    public int create(int itemId, LocalDateTime startTime, LocalDateTime endTime, double minBidStep) {
        String sql = "INSERT INTO auctions (item_id, current_highest_bid, status, start_time, end_time, min_bid_step) "
                + "SELECT id, starting_price, 'OPEN', ?, ?, ? FROM items WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setTimestamp(1, Timestamp.valueOf(startTime));
            ps.setTimestamp(2, Timestamp.valueOf(endTime));
            ps.setDouble   (3, minBidStep);
            ps.setInt      (4, itemId);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Cập nhật giá cao nhất khi có bid mới.
     *return {true} nếu cập nhật thành công (auction đang RUNNING).
     */
    public boolean updateBid(int auctionId, int bidderId, double amount) {
        String sql = "UPDATE auctions SET current_highest_bid = ?, current_winner_id = ? WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDouble(1, amount);
            ps.setInt   (2, bidderId);
            ps.setInt   (3, auctionId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Bắt đầu phiên đấu giá (OPEN → RUNNING), đồng thời chuyển trạng thái vật phẩm tương ứng sang IN_AUCTION.
    public boolean start(int auctionId) {
        String updateAuctionSql = "UPDATE auctions SET status = 'RUNNING' WHERE id = ?";
        String updateItemSql = "UPDATE items SET status = 'IN_AUCTION' WHERE id = (SELECT item_id FROM auctions WHERE id = ?)";
        try (Connection con = db.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(updateAuctionSql);
                 PreparedStatement ps2 = con.prepareStatement(updateItemSql)) {
                ps1.setInt(1, auctionId);
                ps2.setInt(1, auctionId);
                int r1 = ps1.executeUpdate();
                int r2 = ps2.executeUpdate();
                con.commit();
                return r1 > 0 && r2 > 0;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Kết thúc phiên đấu giá — ghi đúng trạng thái (FINISHED hoặc CANCELED) xuống DB.
    public boolean finish(int auctionId, String finalStatus) {
        return setStatus(auctionId, finalStatus);
    }

    /**
     * @deprecated dùng finish(id, status) để truyền đúng trạng thái.
     */
    @Deprecated
    public boolean finish(int auctionId) {
        return setStatus(auctionId, "FINISHED");
    }

    // Lưu bidder đã thắng phiên đấu giá
    public void saveBidderWon(int bidderId, int auctionId) {
        String sql = "INSERT IGNORE INTO bidder_won (bidder_id, auction_id) VALUES (?, ?)";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            ps.setInt(2, auctionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Private helpers

    private boolean setStatus(int auctionId, String status) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE auctions SET status = ? WHERE id = ?")) {

            ps.setString(1, status);
            ps.setInt   (2, auctionId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Thanh toán phiên đấu giá: chuyển auction sang PAID và item sang SOLD trong một transaction.
     * Chỉ áp dụng được khi auction đang ở trạng thái FINISHED.
     */
    public boolean payAuction(int auctionId) {
        String updateAuctionSql = "UPDATE auctions SET status = 'PAID' WHERE id = ? AND status = 'FINISHED'";
        String updateItemSql    = "UPDATE items SET status = 'SOLD' WHERE id = (SELECT item_id FROM auctions WHERE id = ?)";
        try (Connection con = db.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(updateAuctionSql);
                 PreparedStatement ps2 = con.prepareStatement(updateItemSql)) {
                ps1.setInt(1, auctionId);
                ps2.setInt(1, auctionId);
                int r1 = ps1.executeUpdate();
                int r2 = ps2.executeUpdate();
                con.commit();
                return r1 > 0 && r2 > 0;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void markItemInAuction(Connection con, int itemId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE items SET status = 'IN_AUCTION' WHERE id = ?")) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        }
    }
    public boolean updateEndTime(int auctionId, java.time.LocalDateTime newEndTime) {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, newEndTime);
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateEndTime lỗi: " + e.getMessage());
            return false;
        }
    }
    // Thêm hàm trả về một Map chứa các số liệu thống kê cho Admin Dashboard
    public Object getSystemStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        String sqlUser = "SELECT COUNT(*) FROM users";
        String sqlItem = "SELECT COUNT(*) FROM items";
        String sqlAuction = "SELECT COUNT(*) FROM auctions";

        try (java.sql.Connection conn = com.auction.data.DatabaseConnection.getConnection()) {
            // 1. Đếm số lượng Users
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlUser);
                 java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.put("totalUsers", rs.getInt(1));
            }
            // 2. Đếm số lượng sản phẩm
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlItem);
                 java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.put("totalItems", rs.getInt(1));
            }
            // 3. Đếm số lượng phiên đấu giá
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlAuction);
                 java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.put("totalAuctions", rs.getInt(1));
            }

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
}