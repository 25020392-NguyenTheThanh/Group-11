package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.user.User;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.network.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminAuctionListController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnAuctions;
    @FXML private Button btnItems;
    @FXML private Button btnAuditLogs;
    @FXML private Button btnLogout;

    @FXML private VBox dashboardView;
    @FXML private VBox usersView;
    @FXML private VBox auctionsView;
    @FXML private VBox itemsView;
    @FXML private VBox auditLogsView;

    // Dashboard Stats Labels
    @FXML private Label lblTotalUsers;
    @FXML private Label lblUsersSub;
    @FXML private Label lblTotalAuctions;
    @FXML private Label lblAuctionsSub;
    @FXML private Label lblTotalItems;
    @FXML private Label lblActiveSessions;
    @FXML private Label lblTotalRevenue;

    // Active Sessions
    @FXML private ListView<String> listSessions;
    @FXML private Button btnKick;

    // User Management
    @FXML private TextField txtSearchUser;
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Integer> colUserId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colBanReason;
    private final ObservableList<User> usersList = FXCollections.observableArrayList();

    // Auction Management
    @FXML private TextField txtSearchAuction;
    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, Integer> colAuctionId;
    @FXML private TableColumn<Auction, String> colAuctionItem;
    @FXML private TableColumn<Auction, String> colAuctionStart;
    @FXML private TableColumn<Auction, String> colAuctionEnd;
    @FXML private TableColumn<Auction, String> colAuctionStatus;
    @FXML private TableColumn<Auction, Double> colAuctionBid;
    private final ObservableList<Auction> auctionsList = FXCollections.observableArrayList();

    // Item Management
    @FXML private TextField txtSearchItem;
    @FXML private TableView<Item> tableItems;
    @FXML private TableColumn<Item, Integer> colItemId;
    @FXML private TableColumn<Item, String> colItemName;
    @FXML private TableColumn<Item, Double> colItemPrice;
    @FXML private TableColumn<Item, Integer> colItemOwner;
    @FXML private TableColumn<Item, String> colItemStatus;
    private final ObservableList<Item> itemsList = FXCollections.observableArrayList();

    // Audit Logs
    @FXML private ListView<String> listAuditLogs;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUserTable();
        setupAuctionTable();
        setupItemTable();
        refreshStats();
    }

    // --- Tab Switch Logic ---
    @FXML
    private void handleSwitchTab(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();

        btnDashboard.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8;");
        btnUsers.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8;");
        btnAuctions.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8;");
        btnItems.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8;");
        btnAuditLogs.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8;");

        clickedButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffd700;");

        dashboardView.setVisible(false);
        dashboardView.setManaged(false);
        usersView.setVisible(false);
        usersView.setManaged(false);
        auctionsView.setVisible(false);
        auctionsView.setManaged(false);
        itemsView.setVisible(false);
        itemsView.setManaged(false);
        auditLogsView.setVisible(false);
        auditLogsView.setManaged(false);

        if (clickedButton == btnDashboard) {
            dashboardView.setVisible(true);
            dashboardView.setManaged(true);
            refreshStats();
        } else if (clickedButton == btnUsers) {
            usersView.setVisible(true);
            usersView.setManaged(true);
            loadUsers();
        } else if (clickedButton == btnAuctions) {
            auctionsView.setVisible(true);
            auctionsView.setManaged(true);
            loadAuctions();
        } else if (clickedButton == btnItems) {
            itemsView.setVisible(true);
            itemsView.setManaged(true);
            loadItems();
        } else if (clickedButton == btnAuditLogs) {
            auditLogsView.setVisible(true);
            auditLogsView.setManaged(true);
            loadAuditLogs();
        }
    }

    // --- Statistics & Sessions ---
    @FXML
    private void refreshStats() {
        // Stats from server (returns Map)
        Response statsRes = ServerConnection.getInstance().send(RequestType.ADMIN_GET_STATS, null);
        if (statsRes != null && statsRes.isSuccess()) {
            java.util.Map<String, Object> status = (java.util.Map<String, Object>) statsRes.getData();
            if (status != null) {
                int totalUsers = ((Number) status.getOrDefault("totalUsers", 0)).intValue();
                int totalItems = ((Number) status.getOrDefault("totalItems", 0)).intValue();
                int totalAuctions = ((Number) status.getOrDefault("totalAuctions", 0)).intValue();

                lblTotalUsers.setText(String.valueOf(totalUsers));
                lblTotalAuctions.setText(String.valueOf(totalAuctions));
                lblTotalItems.setText(totalItems + " Items");
            }
        }

        // Sessions
        Response sessionsRes = ServerConnection.getInstance().send(RequestType.ADMIN_GET_ACTIVE_SESSIONS, null);
        if (sessionsRes != null && sessionsRes.isSuccess()) {
            List<String> sessions = (List<String>) sessionsRes.getData();
            listSessions.setItems(FXCollections.observableArrayList(sessions));
            lblActiveSessions.setText("Active Sessions: " + (sessions != null ? sessions.size() : 0));
        }

        // Fetch users to compute detailed user status stats
        Response resUsers = ServerConnection.getInstance().send(RequestType.ADMIN_GET_ALL_USERS, null);
        if (resUsers != null && resUsers.isSuccess()) {
            List<User> list = (List<User>) resUsers.getData();
            long bidders = list.stream().filter(u -> "BIDDER".equalsIgnoreCase(u.getRole())).count();
            long sellers = list.stream().filter(u -> "SELLER".equalsIgnoreCase(u.getRole())).count();
            long banned = list.stream().filter(u -> !u.isActive()).count();
            lblUsersSub.setText(String.format("Bidders: %d | Sellers: %d | Banned: %d", bidders, sellers, banned));
        }

        // Fetch auctions to compute detailed auction status stats and system revenue
        Response resAuctions = ServerConnection.getInstance().send(RequestType.ADMIN_GET_ALL_AUCTIONS, null);
        if (resAuctions != null && resAuctions.isSuccess()) {
            List<Auction> list = (List<Auction>) resAuctions.getData();
            long running = list.stream().filter(a -> com.auction.model.auction.AuctionStatus.RUNNING == a.getStatus()).count();
            long finished = list.stream().filter(a -> com.auction.model.auction.AuctionStatus.FINISHED == a.getStatus() || com.auction.model.auction.AuctionStatus.PAID == a.getStatus()).count();
            long canceled = list.stream().filter(a -> com.auction.model.auction.AuctionStatus.CANCELED == a.getStatus()).count();
            lblAuctionsSub.setText(String.format("Running: %d | Finished: %d | Canceled: %d", running, finished, canceled));

            double revenue = list.stream()
                .filter(a -> com.auction.model.auction.AuctionStatus.PAID == a.getStatus())
                .mapToDouble(Auction::getCurrentHighestBid)
                .sum();
            lblTotalRevenue.setText(String.format("$%,.2f", revenue));
        }
    }

    @FXML
    private void handleKickUser() {
        String selectedSession = listSessions.getSelectionModel().getSelectedItem();
        if (selectedSession == null) {
            NotificationController.showAlert("Thông báo", "Vui lòng chọn một session hoạt động để Kick.");
            return;
        }

        // Parse username from "Session ID: xxx | User: username (ROLE)" or similar
        int userIdx = selectedSession.indexOf("User: ");
        if (userIdx != -1) {
            String part = selectedSession.substring(userIdx + 6);
            int parenIdx = part.indexOf(" (");
            if (parenIdx != -1) {
                String username = part.substring(0, parenIdx).trim();
                
                // Fetch user to find their ID
                Response resUsers = ServerConnection.getInstance().send(RequestType.ADMIN_GET_ALL_USERS, null);
                if (resUsers != null && resUsers.isSuccess()) {
                    List<User> list = (List<User>) resUsers.getData();
                    Optional<User> found = list.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
                    if (found.isPresent()) {
                        int userId = found.get().getId();
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Xác nhận Kick");
                        confirm.setHeaderText("Bạn có chắc chắn muốn ngắt kết nối user '" + username + "'?");
                        confirm.setContentText("Hành động này sẽ ngắt kết nối session của người dùng ngay lập tức.");
                        NotificationController.applyDarkTheme(confirm);
                        if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
                            Response resKick = ServerConnection.getInstance().send(RequestType.ADMIN_KICK_USER, new AdminPayload(userId));
                            if (resKick != null && resKick.isSuccess()) {
                                NotificationController.showNotification("Thành công", "Đã ngắt kết nối người dùng thành công.");
                                refreshStats();
                            } else {
                                NotificationController.showAlert("Lỗi", resKick != null ? resKick.getMessage() : "Không thể kick user.");
                            }
                        }
                    }
                }
            }
        }
    }

    // --- User Management ---
    private void setupUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatus()
        ));
        colBanReason.setCellValueFactory(new PropertyValueFactory<>("banReason"));

        FilteredList<User> filteredUsers = new FilteredList<>(usersList, p -> true);
        txtSearchUser.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredUsers.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lower = newValue.toLowerCase();
                return user.getUsername().toLowerCase().contains(lower) || 
                       user.getEmail().toLowerCase().contains(lower);
            });
        });
        tableUsers.setItems(filteredUsers);
    }

    private void loadUsers() {
        Label loadingLabel = new Label("Đang tải dữ liệu người dùng từ database...");
        loadingLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
        tableUsers.setPlaceholder(loadingLabel);
        usersList.clear();

        javafx.concurrent.Task<Response> task = new javafx.concurrent.Task<>() {
            @Override
            protected Response call() {
                return ServerConnection.getInstance().send(RequestType.ADMIN_GET_ALL_USERS, null);
            }
        };

        task.setOnSucceeded(evt -> {
            Response res = task.getValue();
            if (res != null && res.isSuccess()) {
                List<User> list = (List<User>) res.getData();
                if (list != null) {
                    usersList.setAll(list);
                }
            } else {
                tableUsers.setPlaceholder(new Label("Không thể tải danh sách người dùng."));
            }
        });

        task.setOnFailed(evt -> {
            tableUsers.setPlaceholder(new Label("Lỗi kết nối khi tải danh sách người dùng."));
        });

        new Thread(task).start();
    }

    @FXML
    private void handleBanUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationController.showAlert("Thông báo", "Vui lòng chọn người dùng muốn khóa.");
            return;
        }
        if ("ADMIN".equalsIgnoreCase(selected.getRole())) {
            NotificationController.showAlert("Cảnh báo", "Không thể khóa tài khoản Admin khác.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Khóa tài khoản");
        dialog.setHeaderText("Khóa tài khoản '" + selected.getUsername() + "'");
        dialog.setContentText("Nhập lý do khóa:");
        NotificationController.applyDarkTheme(dialog);
        Optional<String> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            String reason = result.get().trim();
            if (reason.isEmpty()) reason = "Vi phạm điều khoản hệ thống";
            Response res = ServerConnection.getInstance().send(RequestType.ADMIN_BAN_USER, new AdminPayload(selected.getId(), reason));
            if (res != null && res.isSuccess()) {
                NotificationController.showNotification("Thành công", "Đã khóa tài khoản thành công!");
                loadUsers();
            } else {
                NotificationController.showAlert("Lỗi", res != null ? res.getMessage() : "Có lỗi xảy ra.");
            }
        }
    }

    @FXML
    private void handleUnbanUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationController.showAlert("Thông báo", "Vui lòng chọn người dùng muốn duyệt hoặc mở khóa.");
            return;
        }
        boolean isPending = "PENDING".equalsIgnoreCase(selected.getStatus());
        Response res = ServerConnection.getInstance().send(RequestType.ADMIN_UNBAN_USER, new AdminPayload(selected.getId()));
        if (res != null && res.isSuccess()) {
            String msg = isPending ? "Đã duyệt tài khoản thành công!" : "Đã mở khóa tài khoản thành công!";
            NotificationController.showNotification("Thành công", msg);
            loadUsers();
        } else {
            NotificationController.showAlert("Lỗi", res != null ? res.getMessage() : "Có lỗi xảy ra.");
        }
    }

    @FXML
    private void handleResetUserPassword() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationController.showAlert("Thông báo", "Vui lòng chọn người dùng muốn reset mật khẩu.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Đặt lại mật khẩu");
        dialog.setHeaderText("Đặt lại mật khẩu cho tài khoản '" + selected.getUsername() + "'");
        dialog.setContentText("Nhập mật khẩu mới:");
        NotificationController.applyDarkTheme(dialog);
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newPass = result.get().trim();
            AdminPayload payload = new AdminPayload();
            payload.targetId = selected.getId();
            payload.newPassword = newPass;

            Response res = ServerConnection.getInstance().send(RequestType.ADMIN_RESET_USER_PASSWORD, payload);
            if (res != null && res.isSuccess()) {
                NotificationController.showNotification("Thành công", "Đã đặt lại mật khẩu thành công!");
                loadUsers();
            } else {
                NotificationController.showAlert("Lỗi", res != null ? res.getMessage() : "Có lỗi xảy ra.");
            }
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationController.showAlert("Thông báo", "Vui lòng chọn người dùng muốn xóa.");
            return;
        }
        if ("ADMIN".equalsIgnoreCase(selected.getRole())) {
            NotificationController.showAlert("Cảnh báo", "Không thể xóa tài khoản Admin.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa vĩnh viễn");
        confirm.setHeaderText("Bạn có chắc muốn xóa vĩnh viễn tài khoản '" + selected.getUsername() + "'?");
        confirm.setContentText("Hành động này sẽ xóa toàn bộ lịch sử và dữ liệu liên quan khỏi database.");
        NotificationController.applyDarkTheme(confirm);
        if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
            Response res = ServerConnection.getInstance().send(RequestType.ADMIN_DELETE_USER, new AdminPayload(selected.getId()));
            if (res != null && res.isSuccess()) {
                NotificationController.showNotification("Thành công", "Đã xóa vĩnh viễn tài khoản!");
                loadUsers();
            } else {
                NotificationController.showAlert("Lỗi", res != null ? res.getMessage() : "Có lỗi xảy ra.");
            }
        }
    }

    // --- Auction Management ---
    private void setupAuctionTable() {
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAuctionItem.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getItem() != null ? cellData.getValue().getItem().getName() : "Unknown Item"
        ));
        colAuctionStart.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStartTime() != null ? cellData.getValue().getStartTime().toString() : ""
        ));
        colAuctionEnd.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getEndTime() != null ? cellData.getValue().getEndTime().toString() : ""
        ));
        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAuctionBid.setCellValueFactory(new PropertyValueFactory<>("currentHighestBid"));

        FilteredList<Auction> filteredAuctions = new FilteredList<>(auctionsList, p -> true);
        txtSearchAuction.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredAuctions.setPredicate(auc -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lower = newValue.toLowerCase();
                return auc.getItem() != null && auc.getItem().getName().toLowerCase().contains(lower);
            });
        });
        tableAuctions.setItems(filteredAuctions);
    }

    private void loadAuctions() {
        Response res = ServerConnection.getInstance().send(RequestType.ADMIN_GET_ALL_AUCTIONS, null);
        if (res != null && res.isSuccess()) {
            List<Auction> list = (List<Auction>) res.getData();
            auctionsList.setAll(list);
        }
    }

    @FXML
    private void handleCancelAuction() {
        Auction selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationController.showAlert("Thông báo", "Vui lòng chọn một phiên đấu giá để hủy.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Hủy phiên đấu giá");
        dialog.setHeaderText("Hủy phiên đấu giá #" + selected.getId() + " - " + (selected.getItem() != null ? selected.getItem().getName() : ""));
        dialog.setContentText("Nhập lý do hủy:");
        NotificationController.applyDarkTheme(dialog);
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String reason = result.get().trim();
            if (reason.isEmpty()) reason = "Hủy bởi Admin hệ thống";
            Response res = ServerConnection.getInstance().send(RequestType.ADMIN_CANCEL_AUCTION, new AdminPayload(selected.getId(), reason));
            if (res != null && res.isSuccess()) {
                NotificationController.showNotification("Thành công", "Đã hủy phiên đấu giá thành công!");
                loadAuctions();
            } else {
                NotificationController.showAlert("Lỗi", res != null ? res.getMessage() : "Có lỗi xảy ra.");
            }
        }
    }

    // --- Item Management ---
    private void setupItemTable() {
        colItemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colItemPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colItemOwner.setCellValueFactory(new PropertyValueFactory<>("ownerId"));
        colItemStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        FilteredList<Item> filteredItems = new FilteredList<>(itemsList, p -> true);
        txtSearchItem.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredItems.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lower = newValue.toLowerCase();
                return item.getName().toLowerCase().contains(lower);
            });
        });
        tableItems.setItems(filteredItems);
    }

    private void loadItems() {
        Response res = ServerConnection.getInstance().send(RequestType.ADMIN_GET_ALL_ITEMS, null);
        if (res != null && res.isSuccess()) {
            List<Item> list = (List<Item>) res.getData();
            itemsList.setAll(list);
        }
    }

    @FXML
    private void handleApproveItem() {
        Item selected = tableItems.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationController.showAlert("Thông báo", "Vui lòng chọn sản phẩm muốn duyệt.");
            return;
        }

        if (selected.getStatus() != com.auction.model.item.ItemStatus.PENDING) {
            NotificationController.showAlert("Thông báo", "Chỉ có thể duyệt sản phẩm ở trạng thái PENDING.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận duyệt sản phẩm");
        confirm.setHeaderText("Bạn có chắc chắn muốn duyệt sản phẩm '" + selected.getName() + "'?");
        confirm.setContentText("Sau khi duyệt, sản phẩm sẽ ở trạng thái AVAILABLE và có thể đem đấu giá.");
        NotificationController.applyDarkTheme(confirm);
        if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
            Response res = ServerConnection.getInstance().send(RequestType.ADMIN_APPROVE_ITEM, new AdminPayload(selected.getId()));
            if (res != null && res.isSuccess()) {
                NotificationController.showNotification("Thành công", "Đã duyệt sản phẩm thành công!");
                loadItems();
            } else {
                NotificationController.showAlert("Lỗi", res != null ? res.getMessage() : "Có lỗi xảy ra.");
            }
        }
    }

    @FXML
    private void handleForceDeleteItem() {
        Item selected = tableItems.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationController.showAlert("Thông báo", "Vui lòng chọn sản phẩm muốn xóa.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa cưỡng chế");
        confirm.setHeaderText("Bạn có chắc chắn muốn xóa cưỡng chế sản phẩm '" + selected.getName() + "'?");
        confirm.setContentText("Lưu ý: Hành động này sẽ xóa sản phẩm kể cả khi đang ở trong phiên đấu giá hoạt động!");
        NotificationController.applyDarkTheme(confirm);
        if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
            Response res = ServerConnection.getInstance().send(RequestType.ADMIN_FORCE_DELETE_ITEM, new AdminPayload(selected.getId()));
            if (res != null && res.isSuccess()) {
                NotificationController.showNotification("Thành công", "Đã xóa cưỡng chế sản phẩm thành công!");
                loadItems();
            } else {
                NotificationController.showAlert("Lỗi", res != null ? res.getMessage() : "Có lỗi xảy ra.");
            }
        }
    }

    // --- Security Audit Logs ---
    @FXML
    private void refreshAuditLogs() {
        loadAuditLogs();
    }

    private void loadAuditLogs() {
        Response res = ServerConnection.getInstance().send(RequestType.ADMIN_GET_AUDIT_LOG, null);
        if (res != null && res.isSuccess()) {
            List<String> logs = (List<String>) res.getData();
            listAuditLogs.setItems(FXCollections.observableArrayList(logs));
        }
    }

    // --- Logout ---
    @FXML
    private void handleLogout(ActionEvent event) {
        ServerConnection.getInstance().stopListening();
        ServerConnection.getInstance().send(RequestType.LOGOUT, null);
        try {
            ServerConnection.getInstance().disconnect();
        } catch (IOException ignored) {}
        GenerationSupport.changeScene(btnLogout, "login-view.fxml", "Welcome to Auction Floor!");
    }
}
