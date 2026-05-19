package com.auction.pattern.factory;

import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;

public class VehicleFactory extends ItemFactory {
    private int year ;
    public VehicleFactory(int year){
        this.year = year ;
    }
    @Override
    public Item createItem(int id,int ownerId , String name , String description , double startingPrice , String imageUrl ){
        return new Vehicle(id ,ownerId, name , description , startingPrice , imageUrl, year);
    }
}
