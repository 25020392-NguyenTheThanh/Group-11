package com.auction.data;

import com.auction.model.item.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
    // lưu item mới - gọi khi seller tạo sản phẩm
    public int save(Item item){
        String sql = "INSERT INTO items (owner_id, name, description, starting_price, category, status, artist, brand, manufacture_year) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql , PreparedStatement.RETURN_GENERATED_KEYS)){
            ps.setInt(1, item.getOwnerId());
            ps.setString(2 , item.getName());
            ps.setString(3 , item.getDescription());
            ps.setDouble(4 , item.getStartingPrice());
            ps.setString(5 , item.getCategory());
            ps.setString(6 , item.getStatus().name());

            // các cột riêng theo từng loại
            if (item instanceof Art a) {
                ps.setString(7, a.getArtist());
                ps.setNull(8, Types.VARCHAR);
                ps.setNull(9, Types.SMALLINT);
            } else if (item instanceof Electronics e) {
                ps.setNull(7, Types.VARCHAR);
                ps.setString(8, e.getBrand());
                ps.setNull(9, Types.SMALLINT);
            } else if (item instanceof Vehicle v) {
                ps.setNull(7, Types.VARCHAR);
                ps.setNull(8, Types.VARCHAR);
                ps.setInt(9, v.getYear());
            } else {
                ps.setNull(7, Types.VARCHAR);
                ps.setNull(8, Types.VARCHAR);
                ps.setNull(9, Types.SMALLINT);
            }
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1); // trả về id MySQL vừa tạo
        } catch (SQLException e){
            System.err.println("Lỗi lưu item : " + e.getMessage());
        }
        return -1 ;
    }
    // cập nhật status item
    public void updateStatus(int itemId , String status){
        String sql = "UPDATE items SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1 , status);
            ps.setInt(2 , itemId);
            ps.executeUpdate();
        } catch (SQLException e){
            System.err.println("Lỗi update status item : " + e.getMessage());
        }
    }
    // Lấy tất cả items — load khi server khởi động
    public List<Item> findAll() {
        String sql = "SELECT * FROM items";
        List<Item> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Lỗi query items: " + e.getMessage());
        }
        return list;
    }
    // lấy item theo owner
    public List<Item> findByOwner(int ownerId){
        String sql = "SELECT * FROM items WHERE owner_id = ?";
        List<Item> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1 , ownerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e ){
            System.err.println("Lỗi findByOwner : " + e.getMessage());
        }
        return list ;
    }
    // lấy item theo id
    public Item findById(int id){
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1 , id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e){
            System.err.println("Lỗi findByItem : " + e.getMessage());
        }
        return null ;
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int ownerId  = rs.getInt("owner_id");
        String name  = rs.getString("name");
        String desc  = rs.getString("description");
        double price  = rs.getDouble("starting_price");
        String category = rs.getString("category");
        String status   = rs.getString("status");
        Item item = switch (category){
            case "ART" -> new Art(id , ownerId , name , desc , price , rs.getString("artist"));
            case "ELECTRONICS" -> new Electronics(id , ownerId , desc , price , rs.getString("brand"));
            case "VEHICLE" -> new Vehicle(id , ownerId , name , desc , price , rs.getInt("year"));
            default -> throw new SQLException("Không rõ loại hàng : " + category);
        };
        item.setStatus(ItemStatus.valueOf(status));
        return item ;
    }
}
