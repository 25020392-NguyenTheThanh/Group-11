package com.example.group11.controller;

import com.auction.client.ServerConnection;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.ChangePasswordPayload;
import com.auction.network.RequestType;
import com.auction.network.Response;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

/**
 * Factory tạo giao diện thông tin cá nhân (Profile View) bằng Java thuần.
 * Hỗ trợ cả 2 role: Bidder (hiển thị số dư, phiên thắng, phiên tham gia)
 * và Seller (hiển thị doanh thu, số sản phẩm).
 *
 * <p>Sử dụng:
 * <pre>
 *   VBox profileView = ProfileViewFactory.create(user, passwordChangeCallback);
 * </pre>
 * </p>
 */
public class ProfileViewFactory {

    // ── Màu sắc theme ─────────────────────────────────────────────────────────
    private static final String BG_DEEP       = "#0A192F";
    private static final String BG_CARD       = "#112240";
    private static final String BG_INPUT      = "#0A192F";
    private static final String BORDER_CARD   = "#1E2D45";
    private static final String GOLD          = "#FFD700";
    private static final String GREEN         = "#22C55E";
    private static final String MUTED         = "#94A3B8";
    private static final String WHITE         = "#FFFFFF";
    private static final String RED_ACCENT    = "#EF4444";

    /**
     * Tạo và trả về VBox chứa toàn bộ giao diện thông tin cá nhân.
     *
     * @param user     Người dùng hiện tại (Bidder hoặc Seller)
     * @param onSaved  Callback nhận message kết quả sau khi đổi mật khẩu thành công/thất bại
     * @return         VBox profile view sẵn sàng nhúng vào layout chính
     */
    public static VBox create(User user, Consumer<String> onSaved) {
        VBox root = new VBox(28);
        root.setPadding(new Insets(35, 40, 35, 40));
        root.setStyle("-fx-background-color: " + BG_DEEP + ";");

        // ── Tiêu đề trang ────────────────────────────────────────────────────
        root.getChildren().add(buildPageTitle());

        // ── Nội dung 2 cột ───────────────────────────────────────────────────
        HBox columns = new HBox(20);
        columns.setFillHeight(true);

        VBox infoCard     = buildInfoCard(user);
        VBox passwordCard = buildPasswordCard(user, onSaved);

        HBox.setHgrow(infoCard,     Priority.ALWAYS);
        HBox.setHgrow(passwordCard, Priority.ALWAYS);

        columns.getChildren().addAll(infoCard, passwordCard);
        root.getChildren().add(columns);

        return root;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TIÊU ĐỀ TRANG
    // ══════════════════════════════════════════════════════════════════════════

    private static VBox buildPageTitle() {
        Label title = new Label("THÔNG TIN CÁ NHÂN");
        title.setTextFill(Color.web(GOLD));
        title.setFont(Font.font("System", FontWeight.BOLD, 26));

        Label subtitle = new Label("Xem thông tin tài khoản và cập nhật bảo mật");
        subtitle.setTextFill(Color.web(MUTED));
        subtitle.setFont(Font.font("System", 13));

        // Divider
        Region divider = new Region();
        divider.setPrefHeight(2);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: linear-gradient(to right, " + GOLD + ", transparent);");

        VBox header = new VBox(6, title, subtitle, divider);
        return header;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARD THÔNG TIN TÀI KHOẢN
    // ══════════════════════════════════════════════════════════════════════════

    private static VBox buildInfoCard(User user) {
        VBox card = createCard();

        // Avatar + tên
        card.getChildren().add(buildAvatarSection(user));
        card.getChildren().add(createDivider());

        // Các dòng thông tin
        String role  = user.getRole();
        boolean isBidder = user instanceof Bidder;
        boolean isSeller = user instanceof Seller;

        card.getChildren().addAll(
            buildInfoRow("🆔  MÃ TÀI KHOẢN",  String.valueOf(user.getId()),   WHITE),
            buildInfoRow("👤  TÊN ĐĂNG NHẬP",  user.getUsername(),              WHITE),
            buildInfoRow("📧  EMAIL",            user.getEmail(),                WHITE),
            buildInfoRow("🎭  VAI TRÒ",          role,                           GOLD)
        );

        if (isBidder) {
            Bidder b = (Bidder) user;
            String balance  = String.format("%,.2f $", b.getBalance());
            int wonCount    = (b.getProfile() != null && b.getProfile().getWonAuctions()        != null)
                              ? b.getProfile().getWonAuctions().size()         : 0;
            int bidCount    = (b.getProfile() != null && b.getProfile().getParticipatedAuctions() != null)
                              ? b.getProfile().getParticipatedAuctions().size() : 0;

            card.getChildren().addAll(
                buildInfoRow("💰  SỐ DƯ VÍ",    balance,               GREEN),
                buildInfoRow("🏆  ĐÃ THẮNG",    wonCount + " phiên",   WHITE),
                buildInfoRow("🎯  ĐÃ THAM GIA", bidCount + " phiên",   WHITE)
            );
        } else if (isSeller) {
            Seller s = (Seller) user;
            String revenue   = String.format("%,.2f $", s.getRevenue());
            int itemsCount   = s.getListedItems() != null ? s.getListedItems().size() : 0;

            card.getChildren().addAll(
                buildInfoRow("📈  DOANH THU",     revenue,                  GREEN),
                buildInfoRow("📦  SẢN PHẨM ĐÃ ĐĂNG", itemsCount + " sản phẩm", WHITE)
            );
        }

        // Thẻ thống kê nhanh (chỉ Bidder)
        if (isBidder) {
            card.getChildren().add(createDivider());
            card.getChildren().add(buildBidderStatsBadges((Bidder) user));
        }

        return card;
    }

    /** Avatar tròn với chữ cái đầu tên + tên hiển thị */
    private static HBox buildAvatarSection(User user) {
        String initial = user.getUsername() != null && !user.getUsername().isEmpty()
                ? String.valueOf(user.getUsername().charAt(0)).toUpperCase() : "?";
        boolean isSeller = user instanceof Seller;
        String avatarColor = isSeller ? "#7C3AED" : "#0EA5E9"; // tím cho Seller, xanh cho Bidder

        // Vòng tròn avatar
        Circle avatar = new Circle(32);
        avatar.setFill(Color.web(avatarColor));
        avatar.setStroke(Color.web(GOLD));
        avatar.setStrokeWidth(2.5);

        Label initLabel = new Label(initial);
        initLabel.setTextFill(Color.WHITE);
        initLabel.setFont(Font.font("System", FontWeight.BOLD, 24));

        StackPane avatarStack = new StackPane(avatar, initLabel);
        avatarStack.setPrefSize(64, 64);

        // Tên & role badge
        Label nameLabel = new Label(user.getUsername());
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 17));

        Label roleBadge = new Label("  " + user.getRole() + "  ");
        roleBadge.setTextFill(Color.web(isSeller ? "#7C3AED" : "#0EA5E9"));
        roleBadge.setStyle("-fx-background-color: " + (isSeller ? "rgba(124,58,237,0.15)" : "rgba(14,165,233,0.15)")
                + "; -fx-background-radius: 6; -fx-border-color: "
                + (isSeller ? "#7C3AED" : "#0EA5E9") + "; -fx-border-radius: 6;");
        roleBadge.setFont(Font.font("System", FontWeight.BOLD, 10));

        VBox nameBox = new VBox(5, nameLabel, roleBadge);
        nameBox.setAlignment(Pos.CENTER_LEFT);

        HBox avatarRow = new HBox(14, avatarStack, nameBox);
        avatarRow.setAlignment(Pos.CENTER_LEFT);
        return avatarRow;
    }

    /** Dòng hiển thị 1 thông tin: label bên trái, giá trị bên phải */
    private static HBox buildInfoRow(String labelText, String valueText, String valueColor) {
        Label lbl = new Label(labelText);
        lbl.setTextFill(Color.web(MUTED));
        lbl.setFont(Font.font("System", FontWeight.BOLD, 10.5));
        lbl.setPrefWidth(160);

        Label val = new Label(valueText);
        val.setTextFill(Color.web(valueColor));
        val.setFont(Font.font("System", FontWeight.BOLD, 13));
        val.setWrapText(true);

        HBox row = new HBox(10, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));
        row.setStyle("-fx-background-color: transparent;");

        // Hover effect
        row.setOnMouseEntered(e -> row.setStyle(
            "-fx-background-color: rgba(255,215,0,0.04); -fx-background-radius: 6;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent;"));

        return row;
    }

    /** Mini stats badges (chỉ Bidder) */
    private static HBox buildBidderStatsBadges(Bidder bidder) {
        int won   = bidder.getProfile() != null ? bidder.getProfile().getWonAuctions().size()         : 0;
        int total = bidder.getProfile() != null ? bidder.getProfile().getParticipatedAuctions().size() : 0;
        double rate = total > 0 ? (won * 100.0 / total) : 0;

        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
            buildStatBadge("🏆", String.valueOf(won),         "Thắng",         "#10B981"),
            buildStatBadge("🎯", String.valueOf(total),        "Tham gia",      "#0EA5E9"),
            buildStatBadge("📊", String.format("%.0f%%", rate), "Tỉ lệ thắng", GOLD)
        );
        return badges;
    }

    private static VBox buildStatBadge(String emoji, String value, String label, String color) {
        Label emojiLbl = new Label(emoji);
        emojiLbl.setFont(Font.font(18));

        Label valueLbl = new Label(value);
        valueLbl.setTextFill(Color.web(color));
        valueLbl.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label labelLbl = new Label(label);
        labelLbl.setTextFill(Color.web(MUTED));
        labelLbl.setFont(Font.font("System", 9));

        VBox badge = new VBox(2, emojiLbl, valueLbl, labelLbl);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(10, 16, 10, 16));
        badge.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 10;"
                + " -fx-border-color: " + BORDER_CARD + "; -fx-border-radius: 10;");
        HBox.setHgrow(badge, Priority.ALWAYS);
        badge.setMaxWidth(Double.MAX_VALUE);
        return badge;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARD ĐỔI MẬT KHẨU
    // ══════════════════════════════════════════════════════════════════════════

    private static VBox buildPasswordCard(User user, Consumer<String> onSaved) {
        VBox card = createCard();

        Label cardTitle = new Label("🔐  Đổi Mật Khẩu");
        cardTitle.setTextFill(Color.web(GOLD));
        cardTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label cardSubtitle = new Label("Cập nhật mật khẩu để bảo vệ tài khoản của bạn");
        cardSubtitle.setTextFill(Color.web(MUTED));
        cardSubtitle.setFont(Font.font("System", 11));
        cardSubtitle.setWrapText(true);

        card.getChildren().addAll(cardTitle, cardSubtitle, createDivider());

        // Fields
        PasswordField currentPf = buildPasswordField("Nhập mật khẩu hiện tại...");
        PasswordField newPf     = buildPasswordField("Nhập mật khẩu mới (tối thiểu 4 ký tự)...");
        PasswordField confirmPf = buildPasswordField("Nhập lại mật khẩu mới...");

        card.getChildren().addAll(
            buildFieldGroup("MẬT KHẨU HIỆN TẠI",   currentPf),
            buildFieldGroup("MẬT KHẨU MỚI",         newPf),
            buildFieldGroup("XÁC NHẬN MẬT KHẨU MỚI", confirmPf)
        );

        // Status label (hiện lỗi / thành công)
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        // Nút cập nhật
        Button updateBtn = buildSubmitButton("CẬP NHẬT MẬT KHẨU");
        updateBtn.setOnAction(e -> handleChangePassword(
            user, currentPf, newPf, confirmPf, statusLabel, updateBtn, onSaved
        ));

        card.getChildren().addAll(statusLabel, updateBtn);

        // Spacer đẩy nội dung lên
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        card.getChildren().add(spacer);

        // Tips bảo mật
        card.getChildren().add(buildSecurityTips());

        return card;
    }

    /** Logic đổi mật khẩu bất đồng bộ */
    private static void handleChangePassword(User user,
                                             PasswordField currentPf,
                                             PasswordField newPf,
                                             PasswordField confirmPf,
                                             Label statusLabel,
                                             Button btn,
                                             Consumer<String> onSaved) {
        String current = currentPf.getText().trim();
        String newPass = newPf.getText().trim();
        String confirm = confirmPf.getText().trim();

        // Validation client-side
        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showStatus(statusLabel, "⚠  Vui lòng điền đầy đủ tất cả các trường!", RED_ACCENT);
            return;
        }
        if (!newPass.equals(confirm)) {
            showStatus(statusLabel, "⚠  Mật khẩu mới và xác nhận không trùng khớp!", RED_ACCENT);
            return;
        }
        if (newPass.length() < 4) {
            showStatus(statusLabel, "⚠  Mật khẩu mới phải có ít nhất 4 ký tự!", RED_ACCENT);
            return;
        }

        btn.setDisable(true);
        btn.setText("Đang cập nhật...");
        showStatus(statusLabel, "⏳  Đang gửi yêu cầu lên máy chủ...", MUTED);

        Task<Response> task = new Task<>() {
            @Override
            protected Response call() {
                return ServerConnection.getInstance().send(
                    RequestType.CHANGE_PASSWORD,
                    new ChangePasswordPayload(current, newPass)
                );
            }
        };

        task.setOnSucceeded(evt -> {
            Response res = task.getValue();
            btn.setDisable(false);
            btn.setText("CẬP NHẬT MẬT KHẨU");

            if (res != null && res.isSuccess()) {
                showStatus(statusLabel, "✅  Đổi mật khẩu thành công!", GREEN);
                currentPf.clear();
                newPf.clear();
                confirmPf.clear();
                // Cập nhật RAM để so khớp đúng các lần sau
                user.setPassWord(com.auction.security.PasswordUtil.hash(newPass));
                if (onSaved != null) onSaved.accept("Đổi mật khẩu thành công!");
            } else {
                String msg = res != null ? res.getMessage() : "Lỗi kết nối";
                showStatus(statusLabel, "❌  " + msg, RED_ACCENT);
            }
        });

        task.setOnFailed(evt -> {
            btn.setDisable(false);
            btn.setText("CẬP NHẬT MẬT KHẨU");
            showStatus(statusLabel, "❌  Lỗi kết nối khi gửi yêu cầu!", RED_ACCENT);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /** Tips bảo mật nhỏ ở cuối card */
    private static VBox buildSecurityTips() {
        VBox tips = new VBox(4);
        tips.setPadding(new Insets(12, 0, 0, 0));
        tips.setStyle("-fx-border-color: " + BORDER_CARD + " transparent transparent transparent;"
                + " -fx-border-width: 1 0 0 0;");

        Label header = new Label("💡  Mẹo bảo mật");
        header.setTextFill(Color.web(MUTED));
        header.setFont(Font.font("System", FontWeight.BOLD, 10));

        for (String tip : new String[]{
            "• Dùng ít nhất 8 ký tự, kết hợp chữ hoa, số và ký tự đặc biệt",
            "• Không dùng lại mật khẩu từ các trang khác",
            "• Đổi mật khẩu định kỳ mỗi 3 tháng"
        }) {
            Label t = new Label(tip);
            t.setTextFill(Color.web("#64748B"));
            t.setFont(Font.font("System", 10));
            tips.getChildren().add(t);
        }

        tips.getChildren().add(0, header);
        return tips;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPER BUILDERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Tạo card container với style chuẩn */
    private static VBox createCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(28, 28, 28, 28));
        card.setStyle(
            "-fx-background-color: " + BG_CARD + ";"
            + "-fx-background-radius: 14;"
            + "-fx-border-color: " + BORDER_CARD + ";"
            + "-fx-border-radius: 14;"
            + "-fx-border-width: 1;"
            + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 12, 0, 0, 4);"
        );
        return card;
    }

    /** Đường kẻ phân cách mảnh */
    private static Region createDivider() {
        Region div = new Region();
        div.setPrefHeight(1);
        div.setMaxWidth(Double.MAX_VALUE);
        div.setStyle("-fx-background-color: " + BORDER_CARD + ";");
        VBox.setMargin(div, new Insets(2, 0, 2, 0));
        return div;
    }

    /** Group: label + password field */
    private static VBox buildFieldGroup(String labelText, PasswordField field) {
        Label lbl = new Label(labelText);
        lbl.setTextFill(Color.web(MUTED));
        lbl.setFont(Font.font("System", FontWeight.BOLD, 10));
        return new VBox(5, lbl, field);
    }

    /** PasswordField với style đồng bộ */
    private static PasswordField buildPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefHeight(40);
        pf.setMaxWidth(Double.MAX_VALUE);
        pf.setStyle(
            "-fx-background-color: " + BG_INPUT + ";"
            + "-fx-text-fill: white;"
            + "-fx-prompt-text-fill: #475569;"
            + "-fx-border-color: " + BORDER_CARD + ";"
            + "-fx-border-radius: 8;"
            + "-fx-background-radius: 8;"
            + "-fx-font-size: 13px;"
        );
        // Focus effect
        pf.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                pf.setStyle(pf.getStyle().replace(
                    "-fx-border-color: " + BORDER_CARD + ";",
                    "-fx-border-color: " + GOLD + ";"
                ));
            } else {
                pf.setStyle(pf.getStyle().replace(
                    "-fx-border-color: " + GOLD + ";",
                    "-fx-border-color: " + BORDER_CARD + ";"
                ));
            }
        });
        return pf;
    }

    /** Nút submit vàng đồng bộ */
    private static Button buildSubmitButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(44);
        btn.setFont(Font.font("System", FontWeight.BOLD, 13));
        btn.setStyle(
            "-fx-background-color: " + GOLD + ";"
            + "-fx-text-fill: #0A192F;"
            + "-fx-background-radius: 10;"
            + "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle()
            .replace("-fx-background-color: " + GOLD, "-fx-background-color: #FFE55C")));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle()
            .replace("-fx-background-color: #FFE55C", "-fx-background-color: " + GOLD)));
        return btn;
    }

    /** Hiện status label với màu tương ứng */
    private static void showStatus(Label lbl, String message, String color) {
        lbl.setText(message);
        lbl.setTextFill(Color.web(color));
        lbl.setVisible(true);
        lbl.setManaged(true);
    }
}
