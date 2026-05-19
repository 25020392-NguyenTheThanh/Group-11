package com.auction.data;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {

    private final DatabaseConnection db;
    private final ItemMapper mapper;

    public ItemRepository() {
        this.db = DatabaseConnection.getInstance();
        this.mapper = new ItemMapper();
    }

    /**
     * Thêm item mới vào database.
     * return id được sinh ra, hoặc {-1} nếu lỗi.
     */
    public int add(Item item) {
        String sql = "INSERT INTO items "
                + "(owner_id, name, description, starting_price, category, imageUrl, artist, brand, manufacture_year) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, item.getOwnerId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setDouble(4, item.getStartingPrice());
            ps.setString(5, item.getCategory());
            bindCategoryFields(ps, item);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Lấy danh sách item thuộc về một seller cụ thể.
    public List<Item> findBySeller(int sellerId) {
        return query("SELECT * FROM items WHERE owner_id = ?", sellerId);
    }

    // Lấy toàn bộ danh sách item.
    public List<Item> findAll() {
        List<Item> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM items")) {

            while (rs.next()) list.add(mapper.map(rs));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Private helpers

    // Gán các tham số đặc thù của từng category (artist / brand / manufacture_year).
    private void bindCategoryFields(PreparedStatement ps, Item item) throws SQLException {
        if (item instanceof Art art) {
            ps.setString(6, art.getArtist());
            ps.setNull  (7, Types.VARCHAR);
            ps.setNull  (8, Types.SMALLINT);
        } else if (item instanceof Electronics elec) {
            ps.setNull  (6, Types.VARCHAR);
            ps.setString(7, elec.getBrand());
            ps.setNull  (8, Types.SMALLINT);
        } else if (item instanceof Vehicle veh) {
            ps.setNull(6, Types.VARCHAR);
            ps.setNull(7, Types.VARCHAR);
            ps.setInt (8, veh.getYear());
        } else {
            ps.setNull(6, Types.VARCHAR);
            ps.setNull(7, Types.VARCHAR);
            ps.setNull(8, Types.SMALLINT);
        }
    }

    // Chạy một PreparedStatement với một tham số int và trả về danh sách Item.
    private List<Item> query(String sql, int param) {
        List<Item> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapper.map(rs));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}