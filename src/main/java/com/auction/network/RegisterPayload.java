package com.auction.network;

import javax.swing.*;
import java.io.Serializable;

public class RegisterPayload implements Serializable {
    public String username , password , email , role ; // role : BIDDER hoặc SELLER
    public RegisterPayload(String username, String password, String email, String role){
        this.username =username;
        this.password = password;
        this.email = email;
        this.role = role;
    }
    public RegisterPayload() {

    }
}
