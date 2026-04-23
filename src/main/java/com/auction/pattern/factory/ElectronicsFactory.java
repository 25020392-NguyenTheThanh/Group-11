package com.auction.pattern.factory;

import com.auction.model.item.Electronics;
import com.auction.model.item.Item;

public class ElectronicsFactory extends ItemFactory {
    private String brand ;
    public ElectronicsFactory(String brand){
        this.brand = brand ;
    }
    @Override
    public Item createItem(int id , String name , String description , double startingPrice ){
        return new Electronics(id , name , description , startingPrice , brand);
    }
}
