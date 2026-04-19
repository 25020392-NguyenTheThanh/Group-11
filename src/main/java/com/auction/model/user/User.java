package com.auction.model.user;

import com.auction.model.entity.Entity;

public abstract class User extends Entity {
    private String username ;
    private String password ;
    private String email ;
    public User(int id , String username , String password , String email){
        super(id);
        this.username = username ;
        this.password = password ;
        this.email = email ;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }

    // trả về các role : BIDDER , SELLER , ADMIN
    public abstract String getRole();

    @Override
    public String getInfo(){
        return "[" + getRole() +"] : " + username + " - " + getId() ;
    }
}
