package com.auction.model.user;

import com.auction.model.entity.Entity;
// fix thêm trạng thái hoạt động!
public abstract class User extends Entity {
    private String username ;
    private String password ;
    private String email ;
    private boolean active; // trạng thái tài khoản
    public User(int id , String username , String password , String email){
        super(id);
        this.username = username ;
        this.password = password ;
        this.email = email ;
        this.active = true;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) {this.active = active;}
    // trả về các role : BIDDER , SELLER , ADMIN
    public abstract String getRole();

    @Override
    public String getInfo(){
        return "[" + getRole() +"] : " + username + " - " + getId() ;
    }
}
