package com.auction.data;

import java.sql.*;

class SchemaInitializer {

    static void run() {
        try (Connection con = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            try (Statement st = con.createStatement()) {
                addColumnIfMissing(meta, st, "users",   "status",         "ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
                addColumnIfMissing(meta, st, "users",   "ban_reason",     "ALTER TABLE users ADD COLUMN ban_reason VARCHAR(255) DEFAULT NULL");
                addColumnIfMissing(meta, st, "bidders", "has_topped_up",  "ALTER TABLE bidders ADD COLUMN has_topped_up TINYINT NOT NULL DEFAULT 0");
                addColumnIfMissing(meta, st, "bidders", "last_top_up_time","ALTER TABLE bidders ADD COLUMN last_top_up_time DATETIME DEFAULT NULL");

                createTableIfMissing(meta, st, "bidder_participated",
                        "CREATE TABLE bidder_participated (" +
                                "bidder_id INT NOT NULL, auction_id INT NOT NULL," +
                                "PRIMARY KEY (bidder_id, auction_id)," +
                                "CONSTRAINT fk_participated_bidder FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                                "CONSTRAINT fk_participated_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE ON UPDATE CASCADE)");

                createTableIfMissing(meta, st, "bidder_won",
                        "CREATE TABLE bidder_won (" +
                                "bidder_id INT NOT NULL, auction_id INT NOT NULL," +
                                "PRIMARY KEY (bidder_id, auction_id)," +
                                "CONSTRAINT fk_won_bidder FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                                "CONSTRAINT fk_won_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE ON UPDATE CASCADE)");

                createTableIfMissing(meta, st, "bid_watchlist",
                        "CREATE TABLE bid_watchlist (" +
                                "bidder_id INT NOT NULL, auction_id INT NOT NULL," +
                                "PRIMARY KEY (bidder_id, auction_id)," +
                                "CONSTRAINT fk_watchlist_bidder FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                                "CONSTRAINT fk_watchlist_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE ON UPDATE CASCADE)");
            }
        } catch (SQLException e) {
            System.err.println("[Database Migration] Error: " + e.getMessage());
        }
    }

    private static void addColumnIfMissing(DatabaseMetaData meta, Statement st,
                                           String table, String column, String ddl) throws SQLException {
        if (!columnExists(meta, table, column)) {
            st.execute(ddl);
            System.out.println("[Database Migration] Added " + column + " column to " + table + " table.");
        }
    }

    private static void createTableIfMissing(DatabaseMetaData meta, Statement st,
                                             String table, String ddl) throws SQLException {
        if (!tableExists(meta, table)) {
            st.execute(ddl);
            System.out.println("[Database Migration] Created table " + table + ".");
        }
    }

    private static boolean tableExists(DatabaseMetaData meta, String name) throws SQLException {
        for (String n : new String[]{name, name.toUpperCase(), name.toLowerCase()}) {
            try (ResultSet rs = meta.getTables(null, null, n, null)) {
                if (rs.next()) return true;
            }
        }
        return false;
    }

    private static boolean columnExists(DatabaseMetaData meta, String table, String column) throws SQLException {
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getColumns(null, null, table.toUpperCase(), column.toUpperCase())) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getColumns(null, null, table.toLowerCase(), column.toLowerCase())) {
            if (rs.next()) return true;
        }
        return false;
    }
}