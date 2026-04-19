package com.auction.model.item;

public class Vehicle extends Item{
    private int year ;
    public Vehicle(int id , String name , String description , double startingPrice , int year){
        super(id , name , description , startingPrice);
        this.year = year ;
    }
    @Override
    public String getCategory(){ return "VEHICLE" ;}
}