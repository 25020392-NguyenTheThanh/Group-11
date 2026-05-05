package com.auction.network;

import java.io.Serializable;

// server đẩy xuống Client(realtime)

public class Notification implements Serializable {
    private static final long serialVersionUID = 1L ;
    private String type ;
    private Object data ;
    public Notification(String type , Object data){
        this.type = type ;
        this.data = data ;
    }
    public String getType() { return type ;}
    public Object getData() { return data ;}
}
