package com.auction.network;

import java.io.Serializable;

public class CreateItemPayload implements Serializable {
    public String type ; // type của item
    public String name ;
    public String description ;
    public double startingPrice ;

    public String artist;        // cho ART
    public String brand;         // cho ELECTRONICS
    public int    year;          // cho VEHICLE
}
