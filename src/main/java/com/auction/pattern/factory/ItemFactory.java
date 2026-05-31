package com.auction.pattern.factory;

import com.auction.model.item.Item;

public abstract class ItemFactory {
    public abstract Item createItem(int id,int ownerId , String name , String description , double startingPrice, String imageUrl);
}
