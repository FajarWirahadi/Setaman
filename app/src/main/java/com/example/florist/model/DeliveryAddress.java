package com.example.florist.model;

import java.io.Serializable;

public class DeliveryAddress implements Serializable {
    private String addressId;
    private String label;
    private String receiverName;
    private String phoneNumber;
    private String fullAddress;
    private String note;
    private boolean isMainAddress;
    private double latitude;
    private double longitude;

    public DeliveryAddress() {
    }

    public DeliveryAddress(String addressId, String label, String receiverName, String phoneNumber, String fullAddress, String note,
                           boolean isMainAddress, double latitude, double longitude) {
        this.addressId = addressId;
        this.label = label;
        this.receiverName = receiverName;
        this.phoneNumber = phoneNumber;
        this.fullAddress = fullAddress;
        this.note = note;
        this.isMainAddress = isMainAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getter dan Setter
    public String getAddressId() { return addressId; }
    public void setAddressId(String addressId) { this.addressId = addressId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getFullAddress() { return fullAddress; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isMainAddress() { return isMainAddress; }
    public void setMainAddress(boolean mainAddress) { isMainAddress = mainAddress; }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}