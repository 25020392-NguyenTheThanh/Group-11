package com.auction.data;

import com.auction.model.auction.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionRepository {

    private final DatabaseConnection db;

    public BidTransactionRepository() {
        this.db = DatabaseConnection.getInstance();
    }

    //Ghi lịch sử một lần đặt giá và đánh dấu bidder đã tham gia phiên.
    public void save(int auctionId, int bidderId, String bidderName, double amount) {
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, bidder_name, amount) VALUES (?,?,?,?)";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt   (1, auctionId);
            ps.setInt   (2, bidderId);
            ps.setString(3, bidderName);
            ps.setDouble(4, amount);
            ps.executeUpdate();

            recordParticipation(con, bidderId, auctionId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Lấy lịch sử đặt giá của một phiên, sắp xếp tăng dần theo thời gian.
    public List<BidTransaction> findByAuction(int auctionId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time ASC";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BidTransaction(
                            rs.getInt("bidder_id"),
                            rs.getString("bidder_name"),
                            rs.getDouble("amount")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Private helpers

    // Ghi vào bidder_participated nếu chưa có bản ghi.
    private void recordParticipation(Connection con, int bidderId, int auctionId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT IGNORE INTO bidder_participated (bidder_id, auction_id) VALUES (?,?)")) {
            ps.setInt(1, bidderId);
            ps.setInt(2, auctionId);
            ps.executeUpdate();
        }
    }
}