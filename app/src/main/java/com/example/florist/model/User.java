package com.example.florist.model;

import java.io.Serializable;

public class User implements Serializable {
    private String userId, shopId;
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
    private boolean hasShop;
    private String address;

    public User() {
    }

    public User(String username, String email, String password, String phoneNumber) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    // 3. Getter & Setter Lengkap
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isHasShop() {return hasShop;}

    public void setHasShop(boolean hasShop) {this.hasShop = hasShop;}

    public String getShopId() {return shopId;}

    public void setShopId(String shopId) {this.shopId = shopId;}

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}