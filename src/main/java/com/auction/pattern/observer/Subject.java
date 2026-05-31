package com.auction.pattern.observer;
// Định nghĩa hành vi quản lý Observer

public interface Subject {
    void addObserver(Observer observer); // Đăng ký nhận thông báo

    void removeObserver(Observer observer);  // Huỷ đăng ký nhận thông báo

    void notifyObservers(String message); // Gửi thông báo
}
