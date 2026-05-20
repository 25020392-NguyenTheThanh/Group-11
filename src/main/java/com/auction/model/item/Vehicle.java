package com.auction.model.item;

public class Vehicle extends Item{
    private final int year ;
    public Vehicle(int id, int ownerId , String name , String description , double startingPrice ,String imageUrl, int year){
        super(id, ownerId , name , description , startingPrice, imageUrl);
        this.year = year ;
    }
    public int getYear(){
        return year;
    }
    @Override
    public String getCategory(){ return "VEHICLE" ;}
    @Override
    public String getInfo() {
        return super.getInfo() + " | Năm SX: " + year;
    }
}