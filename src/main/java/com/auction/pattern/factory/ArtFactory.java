package com.auction.pattern.factory;

import com.auction.model.item.Art;
import com.auction.model.item.Item;

public class ArtFactory extends ItemFactory {
    private String artist ;
    public ArtFactory(String artist){
        this.artist = artist ;
    }
    @Override
    public Item createItem(int id , String name , String description , double startingPrice ){
        return new Art(id , name , description , startingPrice , artist);
    }
}
