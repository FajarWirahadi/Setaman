package com.example.florist.model;

import com.google.firebase.Timestamp;

public class Review {
    private String reviewId;
    private String orderId;
    private String productId;
    private String buyerId;
    private String buyerName;
    private float rating;
    private String comment;
    private Timestamp createdAt;
    private String imageUrl;

    public Review() {}

    public Review(String reviewId, String orderId, String productId, String buyerId, String buyerName, float rating, String comment, String imageUrl) {
        this.reviewId = reviewId;
        this.orderId = orderId;
        this.productId = productId;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = Timestamp.now();
        this.imageUrl = imageUrl;
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}