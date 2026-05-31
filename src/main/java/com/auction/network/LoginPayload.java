package com.auction.network;

import java.io.Serializable;

public class LoginPayload implements Serializable {
    private static final long serialVersionUID = 1L;
    public String username;
    public String password ;
    public LoginPayload(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
