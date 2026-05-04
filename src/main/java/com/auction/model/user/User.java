package com.auction.model.user;

import com.auction.exception.AuthenticationException;
import com.auction.model.entity.Entity;
// fix thêm trạng thái hoạt động!
public abstract class User extends Entity {
    private String username ;
    private String password ;
    private String email ;
    private boolean active; // trạng thái tài khoản
    private boolean authenticated; // trạng thái đăng nhập

    public User(int id , String username , String password , String email){
        super(id);
        this.username = username ;
        this.password = password ;
        this.email = email ;
        this.active = true;
        this.authenticated = false;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }
    public boolean isAuthenticated() { return authenticated; }
    public void setEmail(String email) { this.email = email ; }
    public void setPassWord(String password) { this.password = password ;}

    public void setActive(boolean active) {this.active = active;}
    // trả về các role : BIDDER , SELLER , ADMIN

    public void login(String password) {
        if (!this.password.equals(password)) {
            throw new AuthenticationException("Wrong password");
        }
        this.authenticated = true;
    }

    public void logout() {
        this.authenticated = false;
    }

    public abstract String getRole();

    @Override
    public String getInfo(){
        return "[" + getRole() +"] : " + username + " - " + getId() ;
    }
}
