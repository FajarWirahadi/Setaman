package com.example.florist.model;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Product implements Serializable {
    @DocumentId
    private String id;
    private String productId;
    private String ownerId;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String category;
    private String imageUrl;
    private Date createdAt;
    private double shipping;
    private String duration;
    private String schedule;
    private double rating = 0.0;
    private int rentCount = 0;



    private List<String> gallery = new ArrayList<>();
    private List<String> videoUrls = new ArrayList<>();



    private boolean isActive = true;


    public Product ( ) {}

    public Product(String productId, String ownerId, String name, String description, double price, int stock, String category, String imageUrl) {
        this.productId = productId;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setShipping(double shipping) {
        this.shipping = shipping;
    }

    public double getShipping() {
        return shipping;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDuration() {
        return duration;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getSchedule() {
        return schedule;
    }
    public boolean isActive() {return isActive;}

    public void setActive(boolean active) {isActive = active;}

    public List<String> getGallery() {return gallery;}

    public void setGallery(List<String> gallery) {this.gallery = gallery;}


    public String getId() {return id;}

    public void setId(String id) {this.id = id;}

    public double getRating() {return rating;}

    public void setRating(double rating) {this.rating = rating;}

    public int getRentCount() {return rentCount;}

    public void setRentCount(int rentCount) {this.rentCount = rentCount;}

    public List<String> getVideoUrls() {
        return videoUrls;
    }

    public void setVideoUrls(List<String> videoUrls) {
        this.videoUrls = videoUrls;
    }
}
