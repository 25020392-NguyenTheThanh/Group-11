package com.auction.model.user;

import java.util.List;
import java.util.ArrayList;
public class Seller extends User{
    private  double revenue ; // doanh thu
    private final List <Integer> listedItems ; // id sản phẩm đã đăng
    public Seller(int id, String username, String password, String email) {
        super(id, username, password, email);
        this.revenue = 0;
        this.listedItems = new ArrayList<>() ;
    }
    public double getRevenue(){
        return revenue;
    }
    public void addRevenue( double amount){
        revenue += amount ;
    }
    public void addListItem(int itemId){
        listedItems.add(itemId);
    }
    public List<Integer> getListedItems(){
        return listedItems;
    }
    @Override
    public String getRole(){ return "SELLER" ; }
}
