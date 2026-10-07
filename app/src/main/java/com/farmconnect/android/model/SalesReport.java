package com.farmconnect.android.model;

import java.util.List;

public class SalesReport {
    public long totalOrders;
    public double totalRevenue;
    public long totalUnitsSold;
    public List<ProductSales> topProducts;

    public PeriodSummary today;
    public PeriodSummary weekly;
    public PeriodSummary monthly;
    public PeriodSummary yearly;

    public static class PeriodSummary {
        public long orders;
        public double revenue;
        public long unitsSold;
    }

    public static class ProductSales {
        public long productId;
        public String productName;
        public String unit;
        public long unitsSold;
        public double revenue;
    }
}
