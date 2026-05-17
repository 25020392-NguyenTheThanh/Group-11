package com.auction.data;

import java.sql.*;
import java.time.LocalDateTime;


public class AuctionRepository {

    private final DatabaseConnection db;

    public AuctionRepository() {
        this.db = DatabaseConnection.getInstance();
    }

    /**
     * Tạo phiên đấu giá mới và đổi trạng thái item sang IN_AUCTION.
     * return auctionId được sinh ra, hoặc {-1} nếu lỗi.
     */
    public int create(int itemId, LocalDateTime endTime, double minBidStep) {
        String sql = "INSERT INTO auctions (item_id, current_highest_bid, status, end_time, min_bid_step) "
                + "SELECT id, starting_price, 'OPEN', ?, ? FROM items WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setTimestamp(1, Timestamp.valueOf(endTime));
            ps.setDouble   (2, minBidStep);
            ps.setInt      (3, itemId);
            ps.executeUpdate();

            markItemInAuction(con, itemId);

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
        String sql = "UPDATE auctions SET current_highest_bid = ?, current_winner_id = ? "
                + "WHERE id = ? AND status = 'RUNNING'";
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

    // Bắt đầu phiên đấu giá (OPEN → RUNNING).
    public boolean start(int auctionId) {
        return setStatus(auctionId, "RUNNING");
    }

    // Kết thúc phiên đấu giá (RUNNING → FINISHED).
    public boolean finish(int auctionId) {
        return setStatus(auctionId, "FINISHED");
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

    private void markItemInAuction(Connection con, int itemId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE items SET status = 'IN_AUCTION' WHERE id = ?")) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        }
    }
}