package com.example.florist.model;

public class Shop {
    private String shopId;
    private String ownerId;
    private String shopName;
    private String shopAddress;
    private String shopCity;
    private String shopImageUrl;
    private double rating;

    public Shop() { }

    public Shop(String ownerId, String shopName, String shopAddress, String shopImageUrl) {
        this.ownerId = ownerId;
        this.shopName = shopName;
        this.shopAddress = shopAddress;
        this.shopImageUrl = shopImageUrl;
        this.rating = 0.0;
        this.shopId = ownerId;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopAddress() {
        return shopAddress;
    }

    public void setShopAddress(String shopAddress) {
        this.shopAddress = shopAddress;
    }

    public String getShopCity() {
        return shopCity;
    }

    public void setShopCity(String shopCity) {
        this.shopCity = shopCity;
    }

    public String getShopImageUrl() {
        return shopImageUrl;
    }

    public void setShopImageUrl(String shopImageUrl) {
        this.shopImageUrl = shopImageUrl;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
