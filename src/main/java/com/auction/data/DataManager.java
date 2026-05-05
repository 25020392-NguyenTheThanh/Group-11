package com.auction.data;

import com.auction.model.user.User;

import java.io.*;
import java.util.List;

public class DataManager {
    private static final String Data_dir = "data/" ;
    public synchronized static void saveUsers(List<User> users) {
        save(users , Data_dir + "users.ser");
    }

    public synchronized static List<User> loadUsers(){
        return load(Data_dir + "users.ser");
    }

    private static void save(Object obj , String path) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path)) ){
            oos.writeObject(obj);
        } catch (IOException e){
            System.err.println("Lỗi dữ liệu : " + e.getMessage());
        }
    }
     private static <T> T load(String path){
        File f = new File(path);
        if (!f.exists()) return null ;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))){
            return (T) ois.readObject();
        } catch (Exception e){
            System.err.println("Lỗi : " + e.getMessage());
            return null ;
        }
     }
}
