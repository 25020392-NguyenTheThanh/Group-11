package com.auction.model.item;

import com.auction.model.entity.Entity;

public abstract class Item extends Entity {
    private String name ;
    private String description ;  // mô tả chi tiết về sản phầm
    private double startingPrice ; // giá khởi điểm
    public Item(int id , String name , String description , double startingPrice){
        super(id);
        this.name = name ;
        this.description = description ;
        this.startingPrice = startingPrice ;
    }
    public String getName(){ return name ; }
    public String getDescription(){ return description ;}
    public double getStartingPrice(){ return startingPrice ; }

    // trả về loại sản phẩm : Art , Electronics , Vehicle
    public abstract String getCategory();

    @Override
    public String getInfo(){
        return "[" + getCategory() + "] : " + name + " - " + getId();
    }
}
