package com.farmconnect.android.model;

public class Product {
    public long productId;
    public long farmerId;
    public String farmName;
    public String farmerCity;
    public String productName;
    public String description;
    public String category; // VEGETABLES, FRUITS, GRAINS, ORGANIC_FOODS, OTHER
    public double price;
    public String unit; // KG, GRAM, LITRE, PIECE, DOZEN
    public int quantity;
    public String imageUrl;
    public boolean isOrganic;
    public boolean availability;
    public double averageRating;
    public int reviewCount;
    public String createdAt;
}
