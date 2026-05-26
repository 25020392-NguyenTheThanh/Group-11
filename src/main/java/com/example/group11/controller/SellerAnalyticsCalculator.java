package com.example.group11.controller;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.item.Item;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SellerAnalyticsCalculator {

    public static void loadAnalyticsData(List<Item> auctionItems, Map<Integer, Auction> itemAuctionMap,
                                         BarChart<String, Number> revenueChart, Label totalRevenueLabel,
                                         Label soldProductsLabel, Label totalBidsLabel) {
        revenueChart.getData().clear();

        XYChart.Series<String, Number> totalSeries = new XYChart.Series<>();
        totalSeries.setName("Tổng doanh thu");

        XYChart.Series<String, Number> electronicsSeries = new XYChart.Series<>();
        electronicsSeries.setName("Electronics");

        XYChart.Series<String, Number> artSeries = new XYChart.Series<>();
        artSeries.setName("Art");

        XYChart.Series<String, Number> vehicleSeries = new XYChart.Series<>();
        vehicleSeries.setName("Vehicle");

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        Map<String, Double> totalMap = new LinkedHashMap<>();
        Map<String, Double> electronicsMap = new LinkedHashMap<>();
        Map<String, Double> artMap = new LinkedHashMap<>();
        Map<String, Double> vehicleMap = new LinkedHashMap<>();

        for (String m : months) {
            totalMap.put(m, 0.0);
            electronicsMap.put(m, 0.0);
            artMap.put(m, 0.0);
            vehicleMap.put(m, 0.0);
        }

        double totalRevenue = 0.0;
        int soldCount = 0;
        int totalBids = 0;

        if (auctionItems != null && itemAuctionMap != null) {
            for (Item item : auctionItems) {
                Auction auction = itemAuctionMap.get(item.getId());
                if (auction != null) {
                    if (auction.getBidHistory() != null) {
                        totalBids += auction.getBidHistory().size();
                    }

                    if ((auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID)
                            && auction.getCurrentWinner() != null) {

                        soldCount++;
                        double price = auction.getCurrentHighestBid();
                        if (auction.getStatus() == AuctionStatus.PAID) {
                            totalRevenue += price;

                            String monthAbbr = getMonthAbbreviation(auction.getEndTime());

                            totalMap.put(monthAbbr, totalMap.get(monthAbbr) + price);

                            String category = item.getCategory();
                            if (category != null) {
                                switch (category.toUpperCase()) {
                                    case "ELECTRONICS":
                                        electronicsMap.put(monthAbbr, electronicsMap.get(monthAbbr) + price);
                                        break;
                                    case "ART":
                                        artMap.put(monthAbbr, artMap.get(monthAbbr) + price);
                                        break;
                                    case "VEHICLE":
                                        vehicleMap.put(monthAbbr, vehicleMap.get(monthAbbr) + price);
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (String m : months) {
            totalSeries.getData().add(new XYChart.Data<>(m, totalMap.get(m)));
            electronicsSeries.getData().add(new XYChart.Data<>(m, electronicsMap.get(m)));
            artSeries.getData().add(new XYChart.Data<>(m, artMap.get(m)));
            vehicleSeries.getData().add(new XYChart.Data<>(m, vehicleMap.get(m)));
        }

        CalculatorView.updateCurrency(totalRevenueLabel, totalRevenue);
        CalculatorView.updateCount(soldProductsLabel, soldCount);
        CalculatorView.updateCount(totalBidsLabel, totalBids);

        revenueChart.getData().addAll(totalSeries, electronicsSeries, artSeries, vehicleSeries);
    }

    private static String getMonthAbbreviation(LocalDateTime dateTime) {
        if (dateTime == null) return "Jan";
        return switch (dateTime.getMonth()) {
            case JANUARY -> "Jan";
            case FEBRUARY -> "Feb";
            case MARCH -> "Mar";
            case APRIL -> "Apr";
            case MAY -> "May";
            case JUNE -> "Jun";
            case JULY -> "Jul";
            case AUGUST -> "Aug";
            case SEPTEMBER -> "Sep";
            case OCTOBER -> "Oct";
            case NOVEMBER -> "Nov";
            case DECEMBER -> "Dec";
        };
    }
}
