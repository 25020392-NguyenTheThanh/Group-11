package com.auction.model.item;

public class Electronics extends Item{
    private String brand ; // thương hiệu

    public Electronics(int id , String name , String description , double startingPrice , String brand){
        super(id , name , description , startingPrice);
        this.brand = brand ;
    }

    public String getBrand(){ return brand ; }
    @Override
    public String getCategory(){ return "ELECTRONICS" ;}
}
