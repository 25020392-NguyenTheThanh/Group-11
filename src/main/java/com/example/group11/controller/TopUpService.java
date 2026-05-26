package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.RequestType;
import com.auction.network.Response;
import javafx.concurrent.Task;

public class TopUpService {

    public interface TopUpCallback {
        void onSuccess(double newBalance);
        void onFailure(String message);
    }

    public static boolean checkTopUpLimit(User user) {
        if (user instanceof Bidder bidder) {
            return bidder.canTopUp();
        }
        return true;
    }

    public static void topUp(double amount, User user, TopUpCallback callback) {
        if (user instanceof Bidder bidder) {
            Task<Response> task = new Task<>() {
                @Override
                protected Response call() throws Exception {
                    return ServerConnection.getInstance().send(RequestType.TOP_UP, amount);
                }
            };

            task.setOnSucceeded(evt -> {
                Response res = task.getValue();
                if (res != null && res.isSuccess()) {
                    double newBalance = (Double) res.getData();
                    bidder.topUp(amount);
                    bidder.setLastTopUpTime(java.time.LocalDateTime.now());
                    callback.onSuccess(newBalance);
                } else {
                    callback.onFailure(res != null ? res.getMessage() : "Không thể thực hiện nạp tiền");
                }
            });

            task.setOnFailed(evt -> {
                callback.onFailure("Lỗi kết nối khi gửi yêu cầu nạp tiền");
            });

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        } else {
            callback.onFailure("Người dùng không phải là người mua!");
        }
    }
}
