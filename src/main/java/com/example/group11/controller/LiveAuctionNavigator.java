package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.GetAuctionDetailPayload;
import com.auction.network.RequestType;
import com.auction.network.Response;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LiveAuctionNavigator {

    public static void openLiveAuction(Auction auction, User user, Object controllerCaller) {
        Stage existingStage = LiveAuctionController.getOpenStage(auction.getId());
        if (existingStage != null) {
            Platform.runLater(() -> {
                existingStage.toFront();
                existingStage.requestFocus();
            });
            return;
        }

        Task<Response> task = new Task<>() {
            @Override
            protected Response call() {
                return ServerConnection.getInstance()
                        .send(RequestType.GET_AUCTION_DETAIL, new GetAuctionDetailPayload(auction.getId(), true));
            }
        };

        task.setOnSucceeded(evt -> {
            Response res = task.getValue();
            if (res == null || !res.isSuccess()) {
                NotificationController.showError("Lỗi",
                        "Không thể tải chi tiết phiên: "
                                + (res != null ? res.getMessage() : "mất kết nối"));
                return;
            }

            if (res.getData() instanceof Auction detailed) {
                try {
                    Stage newStage = new Stage();
                    FXMLLoader loader = new FXMLLoader(
                            LiveAuctionNavigator.class.getResource("/com/example/group11/liveAuction-view.fxml"));
                    Parent root = loader.load();
                    LiveAuctionController liveController = loader.getController();

                    BidderAuctionListController bidderListCtrl = (controllerCaller instanceof BidderAuctionListController) ? (BidderAuctionListController) controllerCaller : null;
                    liveController.setPreviousRoot(null, newStage, bidderListCtrl);
                    liveController.setAuctionAndUser(detailed, user);

                    Scene scene = new Scene(root);
                    newStage.setScene(scene);
                    newStage.setTitle("HANK AUCTION - Phòng Đấu Giá: " + detailed.getItem().getName());
                    newStage.show();

                } catch (java.io.IOException e) {
                    NotificationController.showError("Lỗi UI",
                            "Không thể mở màn hình đấu giá.\n" + e.getMessage());
                }
            }
        });

        task.setOnFailed(evt ->
                NotificationController.showError("Lỗi mạng", "Không thể kết nối server.")
        );

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }
}
