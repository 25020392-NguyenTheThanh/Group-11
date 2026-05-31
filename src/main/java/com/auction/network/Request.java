package com.auction.network;

import java.io.Serializable;

// client gửi lên server

public class Request implements Serializable {
    private static final long serialVersionUID = 1L ;
    private RequestType type ;
    private Object payload ; // LoginRequest, PlaceBidRequest,...

    public Request(RequestType type , Object payload ){
        this.type = type ;
        this.payload = payload ;
    }

    public RequestType getType(){ return type; }
    public Object getPayload(){ return payload; }
}
