package com.auction.network;
import  java.io.Serializable;
public class SystemStatus implements Serializable{
    public int    totalUsers;
    public int    totalBidders;
    public int    totalSellers;
    public int    bannedUsers;
    public int    totalAuctions;
    public int    runningAuctions;
    public int    finishedAuctions;
    public int    canceledAuctions;
    public int    totalItems;
    public int    activeSessions;
    public double totalRevenue;

    @Override
    public String toString() {
        return "Users: " + totalUsers + " (Bidders: " + totalBidders + ", Sellers: " + totalSellers + ", Banned: " + bannedUsers + ") | " +
                "Auctions: " + totalAuctions + " (Running: " + runningAuctions + ", Finished: " + finishedAuctions + ", Canceled: " + canceledAuctions + ") | " +
                "Items: " + totalItems + " | Sessions: " + activeSessions + " | Revenue: " + (long) totalRevenue;
    }
}
