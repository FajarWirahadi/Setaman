package com.example.florist.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;

public class Rental implements Serializable {
    // Info Transaksi Dasar
    private String rentalId;
    private String buyerId;
    private String orderId;
    private String buyerName;
    private String receiverName;
    private String sellerId;
    private String sellerName;

    // Info Produk (Tanaman)
    private String plantName;
    private String plantImageUrl;
    private double totalAmount;

    // Info Waktu Penyewaan & Status
    private Timestamp startDate;
    private Timestamp endDate;
    private String status; // "Pending", "Berjalan", "Komplain", "Menunggu Konfirmasi", "Selesai"
    private Timestamp createdAt;
    private Timestamp lastMaintenanceDate;
    private boolean hasComplaint = false;
    private boolean isReviewed = false;
    private DeliveryAddress deliveryAddress;
    private String paymentMethod;
    private String snapToken;
    private String cancellationReason;

    public Rental() {
    }

    public Rental(String rentalId, String orderId, String buyerId, String buyerName,
                  String sellerId, String sellerName, String plantName, String plantImageUrl,
                  double totalAmount, Timestamp startDate, Timestamp endDate, String status) {
        this.rentalId = rentalId;
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.plantName = plantName;
        this.plantImageUrl = plantImageUrl;
        this.totalAmount = totalAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public String getRentalId() {
        return rentalId;
    }

    public void setRentalId(String rentalId) {
        this.rentalId = rentalId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getPlantImageUrl() {
        return plantImageUrl;
    }

    public void setPlantImageUrl(String plantImageUrl) {
        this.plantImageUrl = plantImageUrl;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getLastMaintenanceDate() {
        return lastMaintenanceDate;
    }

    public void setLastMaintenanceDate(Timestamp lastMaintenanceDate) {
        this.lastMaintenanceDate = lastMaintenanceDate;
    }

    public boolean isHasComplaint() {
        return hasComplaint;
    }

    public void setHasComplaint(boolean hasComplaint) {
        this.hasComplaint = hasComplaint;
    }

    public boolean isReviewed() {
        return isReviewed;
    }

    public void setReviewed(boolean reviewed) {
        isReviewed = reviewed;
    }

    public DeliveryAddress getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(DeliveryAddress deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSnapToken() {
        return snapToken;
    }

    public void setSnapToken(String snapToken) {
        this.snapToken = snapToken;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getReceiverName() {
        if (deliveryAddress != null) {
            return deliveryAddress.getReceiverName();
        }
        return buyerName != null ? buyerName : "Tanpa Nama";
    }

    public String getFullDeliveryAddress() {
        if (deliveryAddress != null) {
            return deliveryAddress.getFullAddress();
        }
        return "Alamat tidak ditemukan";
    }
}