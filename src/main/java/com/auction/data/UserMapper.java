package com.auction.data;

import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

        return switch (role) {
            case "ADMIN" -> new Admin(id, username, password, email);

            case "SELLER" -> {
                double revenue = fetchSellerRevenue(con, id);
                Seller s = new Seller(id, username, password, email);
                s.addRevenue(revenue);
                yield s;
            }

            default -> { // BIDDER
                double balance = fetchBidderBalance(con, id);
                yield new Bidder(id, username, password, email, balance);
            }
        };
    }

    // private helpers

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
}