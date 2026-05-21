package com.auction.data;

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
                LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                double minBidStep = rs.getDouble("min_bid_step");

                com.auction.model.item.Item item = com.auction.manager.ItemManager.getInstance().findItem(itemId);
                if (item == null) continue;

                com.auction.model.auction.Auction auction = new com.auction.model.auction.Auction(id, item, endTime, minBidStep);
                auction.restoreStatus(com.auction.model.auction.AuctionStatus.valueOf(statusStr));
                auction.restoreHighestBid(currentHighestBid);

                if (!winnerIsNull && winnerId > 0) {
                    com.auction.model.user.User winner = com.auction.manager.UserManager.getInstance().findUserById(winnerId);
                    if (winner instanceof com.auction.model.user.Bidder b) {
                        //auction.restoreCurrentWinner(b);
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