package com.auction.network;

import java.io.Serializable;

public class UpdateItemPayload implements Serializable {
    public int id;
    public String type; // type của item (Electronics, Art, Vehicle)
    public String name;
    public String description;
    public double startingPrice;
    public String imageUrl;

    public String artist;        // cho ART
    public String brand;         // cho ELECTRONICS
    public int    year;          // cho VEHICLE
}
