package com.auction.network;

import java.io.Serializable;

public class ChangePasswordPayload implements Serializable {
    private static final long serialVersionUID = 1L;
    public String oldPassword;
    public String newPassword;

    public ChangePasswordPayload(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
