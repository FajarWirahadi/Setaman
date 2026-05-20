package com.example.florist.model;

import java.io.Serializable;
import com.google.firebase.Timestamp;
import java.util.Date;
import java.util.List;

public class Order implements Serializable {
    private String orderId;
    private String buyerId;
    private String sellerId;
    private List<CartItem> items;
    private double totalAmount;
    private String status;
    private String snapToken;
    private Timestamp createdAt;
    private DeliveryAddress deliveryAddress;
    private String paymentMethod;
    private String cancellationReason;

    public Order() {
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // 2. Konstruktor Penuh
    public Order(String orderId, String buyerId, String sellerId, List<CartItem> items, double totalAmount, DeliveryAddress deliveryAddress, String paymentMethod) {
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = "PENDING";
        this.createdAt = Timestamp.now();
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getSnapToken() {
        return snapToken;
    }

    public void setSnapToken(String snapToken) {
        this.snapToken = snapToken;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }


    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public DeliveryAddress getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(DeliveryAddress deliveryAddress) { this.deliveryAddress = deliveryAddress; }


    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReceiverName() {
        if (deliveryAddress != null) {
            return deliveryAddress.getReceiverName();
        }
        return "Tanpa Nama";
    }

    public String getFullDeliveryAddress() {
        if (deliveryAddress != null) {
            return deliveryAddress.getFullAddress();
        }
        return "Alamat tidak ditemukan";
    }


}