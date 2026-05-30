package com.auction.server;

import com.auction.network.Request;
import com.auction.network.Response;
import com.auction.server.handler.*;

public class RequestProcessor {

    public static Response process(Request request, ClientHandler handler) {
        try {
            return switch (request.getType()) {
                // Auth
                case LOGIN           -> AuthHandler.handleLogin(request, handler);
                case REGISTER        -> AuthHandler.handleRegister(request, handler);
                case LOGOUT          -> AuthHandler.handleLogout(handler);
                case VERIFY_EMAIL    -> AuthHandler.handleVerifyEmail(request);
                case RESET_PASSWORD  -> AuthHandler.handleResetPassword(request);
                case CHANGE_PASSWORD -> AuthHandler.handleChangePassword(request, handler);

                // Auction
                case GET_AUCTIONS       -> AuctionHandler.handleGetAuctions();
                case GET_AUCTION_DETAIL -> AuctionHandler.handleGetAuctionDetail(request, handler);
                case PLACE_BID          -> AuctionHandler.handlePlaceBid(request, handler);
                case CREATE_AUCTION     -> AuctionHandler.handleCreateAuction(request, handler);
                case SET_AUTO_BID       -> AuctionHandler.handleSetAutoBid(request, handler);
                case CANCEL_AUTO_BID    -> AuctionHandler.handleCancelAutoBid(request, handler);

                // Item
                case CREATE_ITEM  -> ItemHandler.handleCreateItem(request, handler);
                case GET_MY_ITEMS -> ItemHandler.handleGetMyItems(handler);
                case DELETE_ITEM  -> ItemHandler.handleDeleteItem(request, handler);
                case UPDATE_ITEM  -> ItemHandler.handleUpdateItem(request, handler);

                // Bidder
                case ADD_TO_WATCHLIST      -> BidderHandler.handleAddToWatchlist(request, handler);
                case REMOVE_FROM_WATCHLIST -> BidderHandler.handleRemoveFromWatchlist(request, handler);
                case TOP_UP                -> BidderHandler.handleTopUp(request, handler);
                case CONFIRM_PAYMENT       -> BidderHandler.handleConfirmPayment(request, handler);
                case DECLINE_PAYMENT       -> BidderHandler.handleDeclinePayment(request, handler);

                // Admin
                case ADMIN_GET_ALL_USERS       -> AdminHandler.handleGetAllUsers(handler);
                case ADMIN_BAN_USER            -> AdminHandler.handleBanUser(request, handler);
                case ADMIN_UNBAN_USER          -> AdminHandler.handleUnbanUser(request, handler);
                case ADMIN_DELETE_USER         -> AdminHandler.handleDeleteUser(request, handler);
                case ADMIN_RESET_USER_PASSWORD -> AdminHandler.handleResetUserPassword(request, handler);
                case ADMIN_GET_ALL_AUCTIONS    -> AdminHandler.handleGetAllAuctions(handler);
                case ADMIN_CANCEL_AUCTION      -> AdminHandler.handleCancelAuction(request, handler);
                case ADMIN_GET_ALL_ITEMS       -> AdminHandler.handleGetAllItems(handler);
                case ADMIN_FORCE_DELETE_ITEM   -> AdminHandler.handleForceDeleteItem(request, handler);
                case ADMIN_APPROVE_ITEM        -> AdminHandler.handleApproveItem(request, handler);
                case ADMIN_GET_STATS           -> AdminHandler.handleGetStats(handler);
                case ADMIN_GET_AUDIT_LOG       -> AdminHandler.handleGetAuditLog(handler);
                case ADMIN_GET_ACTIVE_SESSIONS -> AdminHandler.handleGetActiveSessions(handler);
                case ADMIN_KICK_USER           -> AdminHandler.handleKickUser(request, handler);
            };
        } catch (Exception e) {
            System.err.println("[RequestProcessor] Unhandled: " + request.getType() + " → " + e.getMessage());
            return Response.error("Lỗi xử lý yêu cầu: " + e.getMessage());
        }
    }
}