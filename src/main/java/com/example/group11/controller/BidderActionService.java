package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.auction.Auction;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.network.PlaceBidPayload;
import com.auction.network.RequestType;
import com.auction.network.Response;
import javafx.concurrent.Task;

public class BidderActionService {

    public interface ActionCallback {
        void onSuccess(Response res);
        void onFailure(Throwable t);
    }

    public static void placeBid(Auction auction, double bidAmount, User user, ActionCallback callback) {
        Task<Response> bidTask = new Task<>() {
            @Override
            protected Response call() throws Exception {
                PlaceBidPayload payload = new PlaceBidPayload();
                payload.auctionId = auction.getId();
                payload.amount = bidAmount;
                return ServerConnection.getInstance().send(RequestType.PLACE_BID, payload);
            }
        };

        bidTask.setOnSucceeded(evt -> {
            Response res = bidTask.getValue();
            callback.onSuccess(res);
        });

        bidTask.setOnFailed(evt -> {
            callback.onFailure(bidTask.getException());
        });

        Thread t = new Thread(bidTask);
        t.setDaemon(true);
        t.start();
    }

    public static void confirmPayment(Auction auction, ActionCallback callback) {
        Task<Response> payTask = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return ServerConnection.getInstance().send(RequestType.CONFIRM_PAYMENT, auction.getId());
            }
        };

        payTask.setOnSucceeded(evt -> {
            Response res = payTask.getValue();
            callback.onSuccess(res);
        });

        payTask.setOnFailed(evt -> {
            callback.onFailure(payTask.getException());
        });

        Thread t = new Thread(payTask);
        t.setDaemon(true);
        t.start();
    }

    public static void toggleWatchlist(Auction auction, User user, boolean isWatched, ActionCallback callback) {
        RequestType type = isWatched ? RequestType.REMOVE_FROM_WATCHLIST : RequestType.ADD_TO_WATCHLIST;

        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return ServerConnection.getInstance().send(type, auction.getId());
            }
        };

        task.setOnSucceeded(evt -> {
            Response res = task.getValue();
            callback.onSuccess(res);
        });

        task.setOnFailed(evt -> {
            callback.onFailure(task.getException());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
