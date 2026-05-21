package com.auction.data;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.item.Vehicle;

import java.sql.ResultSet;
import java.sql.SQLException;

//Ánh xạ một hàng trong ResultSet sang đối tượng Item phù hợp (Art / Electronics / Vehicle).

public class ItemMapper {

    // Đọc hàng hiện tại của {rs} và trả về đối tượng Item tương ứng.
    public Item map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int ownerId = rs.getInt("owner_id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        double startPrice = rs.getDouble("starting_price");
        String category = rs.getString("category");
        String imageUrl = rs.getString("image_url");
        String status = rs.getString("status");

        Item item = switch (category) {
            case "ART" -> new Art(id, ownerId, name, description, startPrice, imageUrl,
                    rs.getString("artist"));
            case "ELECTRONICS" -> new Electronics(id, ownerId, name, description, startPrice, imageUrl,
                    rs.getString("brand"));
            case "VEHICLE" -> new Vehicle(id, ownerId, name, description, startPrice, imageUrl,
                    rs.getInt("manufacture_year"));
            default -> throw new IllegalStateException("Category không hợp lệ: " + category);
        };

        if (status != null) {
            item.setStatus(ItemStatus.valueOf(status));
        }
        return item;
    }
}
