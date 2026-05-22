package com.auction.pattern.factory;

import com.auction.model.item.Art;
import com.auction.model.item.Item;

public class ArtFactory extends ItemFactory {
    private String artist ;
    public ArtFactory(String artist){
        this.artist = artist ;
    }
    @Override
    public Item createItem(int id, int ownerId , String name , String description , double startingPrice, String imageUrl ){
        return new Art(id ,ownerId, name , description , startingPrice ,imageUrl, artist);
    }
}
