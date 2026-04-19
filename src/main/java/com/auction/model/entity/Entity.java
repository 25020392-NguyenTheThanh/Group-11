package com.auction.model.entity;

import java.time.LocalDateTime;

public abstract class Entity {
    private int id ;
    private LocalDateTime createAt ; // thời gian tạo ra obj
    public Entity(int id){
        this.id = id ;
        this.createAt = LocalDateTime.now();
    }
    public int getId(){ return id; }

    public LocalDateTime getCreateAt() { return createAt ; }

    public abstract String getInfo();
}
