package com.example.florist.model;

import java.io.Serializable;
import java.util.Date;

public class CartItem implements Serializable {
    private String productId;
    private String name;
    private double price;
    private String imageUrl;
    private String ownerId;
    private String shopName;
    private String durationType;
    private int durationValue;
    private int quantity;
    private Date addedAt;

    public CartItem() {
    }

    public CartItem(String productId, String name, double price, String imageUrl, String ownerId, String shopName,
                    int quantity, String durationType, int durationValue, Date addedAt) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.ownerId = ownerId;
        this.quantity = quantity;
        this.durationType = durationType;
        this.durationValue = durationValue;
        this.addedAt = addedAt;
        this.shopName = shopName;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDurationType() {
        return durationType;
    }

    public void setDurationType(String durationType) {
        this.durationType = durationType;
    }

    public int getDurationValue() {
        return durationValue;
    }

    public void setDurationValue(int durationValue) {
        this.durationValue = durationValue;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public Date getAddedAt() { return addedAt; }
    public void setAddedAt(Date addedAt) { this.addedAt = addedAt; }
}