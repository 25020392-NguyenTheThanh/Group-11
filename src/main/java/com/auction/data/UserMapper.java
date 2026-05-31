package com.auction.data;

import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Ánh xạ một hàng trong ResultSet sang đối tượng User phù hợp (Admin / Seller / Bidder).
public class UserMapper {

    /**
     * Đọc hàng hiện tại của {rs} và trả về đối tượng User tương ứng.
     * Cần {con} để truy vấn bảng phụ (bidders / sellers).
     */
    public User map(Connection con, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email = rs.getString("email");
        String role = rs.getString("role");
        String status = rs.getString("status");
        String banReason = rs.getString("ban_reason");

        User user = switch (role) {
            case "ADMIN" -> new Admin(id, username, password, email);

            case "SELLER" -> {
                double revenue = fetchSellerRevenue(con, id);
                Seller s = new Seller(id, username, password, email);
                s.addRevenue(revenue);
                yield s;
            }

            default -> { // BIDDER
                double balance = fetchBidderBalance(con, id);
                LocalDateTime lastTopUpTime = fetchBidderLastTopUpTime(con, id);
                Bidder b = new Bidder(id, username, password, email, balance, lastTopUpTime);
                List<Integer> participated = fetchBidderParticipated(con, id);
                for (int auctionId : participated) {
                    b.getProfile().addParticipatedAuction(auctionId);
                }
                List<Integer> watchlist = fetchBidderWatchlist(con, id);
                for (int auctionId : watchlist) {
                    b.getProfile().addToWatchlist(auctionId);
                }
                List<Integer> won = fetchBidderWon(con, id);
                for (int auctionId : won) {
                    b.getProfile().addWonAuction(auctionId);
                }
                yield b;
            }
        };

        if (user != null) {
            user.setStatus(status != null ? status : "ACTIVE");
            user.setActive("ACTIVE".equalsIgnoreCase(status));
            user.setBanReason(banReason != null ? banReason : "");
        }
        return user;
    }

    // private helpers

    private LocalDateTime fetchBidderLastTopUpTime(Connection con, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT last_top_up_time FROM bidders WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet r = ps.executeQuery()) {
                if (r.next()) {
                    Timestamp ts = r.getTimestamp("last_top_up_time");
                    if (ts != null) {
                        return ts.toLocalDateTime();
                    }
                }
                return null;
            }
        }
    }

    private double fetchSellerRevenue(Connection con, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT revenue FROM sellers WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet r = ps.executeQuery();
            return r.next() ? r.getDouble("revenue") : 0;
        }
    }

    private double fetchBidderBalance(Connection con, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT balance FROM bidders WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet r = ps.executeQuery();
            return r.next() ? r.getDouble("balance") : 0;
        }
    }

    private List<Integer> fetchBidderParticipated(Connection con, int userId) throws SQLException {
        List<Integer> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT auction_id FROM bidder_participated WHERE bidder_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    list.add(r.getInt("auction_id"));
                }
            }
        }
        return list;
    }

    private List<Integer> fetchBidderWatchlist(Connection con, int userId) throws SQLException {
        List<Integer> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT auction_id FROM bid_watchlist WHERE bidder_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    list.add(r.getInt("auction_id"));
                }
            }
        }
        return list;
    }

    private List<Integer> fetchBidderWon(Connection con, int userId) throws SQLException {
        List<Integer> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT auction_id FROM bidder_won WHERE bidder_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    list.add(r.getInt("auction_id"));
                }
            }
        }
        return list;
    }
}