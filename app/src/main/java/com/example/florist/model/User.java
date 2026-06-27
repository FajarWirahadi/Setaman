package com.example.florist.model;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {
    private String userId, shopId;
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
    private boolean hasShop;
    private String address;
    private String profileImageUrl;

    public User() {
        // Constructor kosong wajib untuk Firebase Firestore
    }

    public User(String username, String email, String password, String phoneNumber) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    protected User(Parcel in) {
        userId = in.readString();
        shopId = in.readString();
        username = in.readString();
        email = in.readString();
        password = in.readString();
        phoneNumber = in.readString();
        hasShop = in.readByte() != 0; // Membaca boolean
        address = in.readString();
        profileImageUrl = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    // --- Getter & Setter ---
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isHasShop() { return hasShop; }
    public void setHasShop(boolean hasShop) { this.hasShop = hasShop; }

    public String getShopId() { return shopId; }
    public void setShopId(String shopId) { this.shopId = shopId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    // --- Parcelable Implementation ---
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(shopId);
        dest.writeString(username);
        dest.writeString(email);
        dest.writeString(password);
        dest.writeString(phoneNumber);
        dest.writeByte((byte) (hasShop ? 1 : 0)); // Menulis boolean
        dest.writeString(address);
        dest.writeString(profileImageUrl);
    }
}