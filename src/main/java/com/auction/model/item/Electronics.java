package com.auction.model.item;

public class Electronics extends Item{
    private String brand ; // thương hiệu

    public Electronics(int id, int ownerId , String name , String description , double startingPrice , String brand){
        super(id , ownerId, name , description , startingPrice);
        this.brand = brand ;
    }
    public String getBrand(){ return brand ; }
    @Override
    public String getCategory(){ return "ELECTRONICS" ;}
    @Override
    public String getInfo() {
        return super.getInfo() + " | Thương hiệu: " + brand;
    }
}

