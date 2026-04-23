package com.auction.pattern.factory;

import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;

public class VehicleFactory extends ItemFactory {
    private int year ;
    public VehicleFactory(int year){
        this.year = year ;
    }
    @Override
    public Item createItem(int id , String name , String description , double startingPrice ){
        return new Vehicle(id , name , description , startingPrice , year);
    }
}
