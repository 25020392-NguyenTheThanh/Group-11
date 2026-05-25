package com.auction.network;

import java.io.Serializable;

public class ResetPasswordPayload implements Serializable {
    private static final long serialVersionUID = 1L;
    public String username;
    public String newPassword;

    public ResetPasswordPayload(String username, String newPassword) {
        this.username = username;
        this.newPassword = newPassword;
    }
}
