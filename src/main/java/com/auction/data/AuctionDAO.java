package com.auction.data;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.auction.BidTransaction;
import com.auction.model.item.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
    // lưu phiên mới
    public int save(Auction auction) {
        String sql = "INSERT INTO auctions (item_id, current_highest_bid, status, end_time, min_bid_step) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, auction.getItem().getId());
            ps.setDouble(2, auction.getCurrentHighestBid());
            ps.setString(3, auction.getStatus().name());
            ps.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
            ps.setDouble(5, auction.getMinBidStep());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);

        } catch (SQLException e) {
            System.err.println("Lỗi lưu auction: " + e.getMessage());
        }
        return -1;
    }
    // Cập nhật sau mỗi lần đặt giá hoặc kết thúc phiên
    public void update(Auction auction) {
        String sql = "UPDATE auctions SET current_highest_bid = ?, current_winner_id = ?, status = ? "
                + "WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, auction.getCurrentHighestBid());
            if (auction.getCurrentWinner() != null)
                ps.setInt(2, auction.getCurrentWinner().getId());
            else
                ps.setNull(2, Types.INTEGER);
            ps.setString(3, auction.getStatus().name());
            ps.setInt(4, auction.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Lỗi update auction: " + e.getMessage());
        }
    }
    // Lưu lịch sử mỗi lần đặt giá
    public void saveBidTransaction(int auctionId, BidTransaction tx) {
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, bidder_name, amount) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            ps.setInt(2, tx.getBidderId());
            ps.setString(3, tx.getBidderName());
            ps.setDouble(4, tx.getAmount());
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Lỗi lưu bid transaction: " + e.getMessage());
        }
    }
    // Load tất cả auctions khi server khởi động
    public List<Auction> findAll(ItemDAO itemDAO) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                Item item  = itemDAO.findById(itemId); // lấy Item từ DB
                if (item == null) continue;

                Auction auction = new Auction(
                        rs.getInt("id"),
                        item,
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        rs.getDouble("min_bid_step")
                );

                // Restore trạng thái
                auction.restoreStatus(AuctionStatus.valueOf(rs.getString("status")));
                auction.restoreHighestBid(rs.getDouble("current_highest_bid"));

                list.add(auction);
            }

        } catch (SQLException e) {
            System.err.println("Lỗi load auctions: " + e.getMessage());
        }
        return list;
    }
}
