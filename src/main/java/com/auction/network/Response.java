package com.auction.network;

import java.io.Serializable;

// server trả về client

public class Response implements Serializable {
    private static final long serialVersionUID = 1L ;
    private boolean success ;
    private String message ;
    private Object data ; // dữ liệu người dùng

    public static Response ok(Object data){ // xác nhận thành công
        Response r = new Response();
        r.success = true ;
        r.data = data ;
        return r ;
    }
    public static Response error(String message) {
        Response r = new Response();
        r.success = false;
        r.message = message;
        return r;
    }
    public boolean isSuccess(){ return success ;}
    public String getMessage(){ return message ; }
    public Object getData(){ return data ; }
}
