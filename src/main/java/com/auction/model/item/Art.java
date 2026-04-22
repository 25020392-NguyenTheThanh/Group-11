package com.auction.model.item;

public class Art extends Item{
    private String artist ; // họa sĩ

    public Art(int id , String name , String description , double startingPrice , String artist){
        super(id , name , description , startingPrice);
        this.artist = artist ;
    }

    public String getArtist(){ return artist ; }

    @Override
    public String getCategory(){ return "ART" ;}
}
